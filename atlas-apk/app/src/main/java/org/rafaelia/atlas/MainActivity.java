/* SPDX-License-Identifier: GPL-3.0-or-later */
package org.rafaelia.atlas;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.HashSet;

/** Hosted Android SAF boundary; L0 classifier is in atlas_core.c. No network permissions. */
public final class MainActivity extends Activity {
    private static final int PICK_TREE=101, SAVE_INDEX=102;
    private static final String[] COLUMNS={
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
        DocumentsContract.Document.COLUMN_SIZE,
        DocumentsContract.Document.COLUMN_LAST_MODIFIED
    };
    private TextView status;
    private Button pick, save;
    private File output;
    private volatile boolean busy;
    private static native long classifyName(String name, boolean directory);
    static { System.loadLibrary("rafaelia_atlas"); }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll=new ScrollView(this);
        LinearLayout layout=new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad=(int)(16*getResources().getDisplayMetrics().density);
        layout.setPadding(pad,pad,pad,pad);
        TextView title=new TextView(this);
        title.setText("RAFAELIA Atlas Ω — Piloto local v0.1");
        title.setTextSize(21);
        layout.addView(title);
        status=new TextView(this);
        status.setText("Escolha uma pasta. Somente metadados; arquivos originais intactos. Limites: "
            +InventorySafety.MAX_OBJECTS+" objetos, "+InventorySafety.MAX_DEPTH+" níveis; "+InventorySafety.MAX_ROWS+" linhas observadas.\nExportação pode conter nomes privados.");
        layout.addView(status);
        pick=new Button(this);
        pick.setText("Selecionar pasta e inventariar");
        pick.setOnClickListener(v->{
            if(busy) return;
            Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(i,PICK_TREE);
        });
        layout.addView(pick);
        save=new Button(this);
        save.setText("Exportar catálogo JSONL (destino escolhido)");
        save.setEnabled(false);
        save.setOnClickListener(v->{
            if(output==null||busy) return;
            Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);
            i.setType("application/x-ndjson");
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.putExtra(Intent.EXTRA_TITLE,"rafaelia-atlas-catalog-v1.jsonl");
            startActivityForResult(i,SAVE_INDEX);
        });
        layout.addView(save);
        scroll.addView(layout);
        setContentView(scroll);
    }

    @Override protected void onActivityResult(int req,int result,Intent data) {
        super.onActivityResult(req,result,data);
        if(result!=RESULT_OK||data==null||data.getData()==null)return;
        Uri uri=data.getData();
        if(req==PICK_TREE) {
            try {
                getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch(SecurityException e) {
                status.setText("Permissão persistente indisponível: seleção desta sessão.");
            }
            busy=true; output=null;
            pick.setEnabled(false); save.setEnabled(false);
            status.setText("Inventariando...");
            new Thread(()->scan(uri),"atlas-saf-scan").start();
        } else if(req==SAVE_INDEX&&output!=null) {
            File snapshot=output;
            busy=true; pick.setEnabled(false); save.setEnabled(false);
            new Thread(()->{
                try(FileInputStream source=new FileInputStream(snapshot);
                    OutputStream target=getContentResolver().openOutputStream(uri,"w")) {
                    if(target==null)throw new java.io.IOException("Destino indisponível");
                    byte[] buffer=new byte[8192]; int n;
                    while((n=source.read(buffer))!=-1)target.write(buffer,0,n);
                    target.flush();
                    show("Exportado ao destino selecionado. Não comprova publicação Drive nem bytes de origem.",true);
                } catch(Exception e) {
                    show("Falha na exportação: "+e.getClass().getSimpleName(),true);
                }
            },"atlas-export").start();
        }
    }

    private static final class Pending {
        final String id,path;
        final int depth;
        Pending(String i,String p,int d){id=i;path=p;depth=d;}
    }
    private void scan(Uri tree) {
        long files=0,dirs=0,bytes=0;
        int records=0,errors=0,rowsSeen=0,repeatedIds=0,unknownSizes=0;
        boolean truncated=false,overflow=false;
        File temp=new File(getCacheDir(),"atlas-index-v1.jsonl");
        try {
            MessageDigest digest=MessageDigest.getInstance("SHA-256");
            String rootId=DocumentsContract.getTreeDocumentId(tree);
            if(!InventorySafety.validRootId(rootId))
                throw new java.io.IOException("Invalid SAF root identity");
            ArrayDeque<Pending> queue=new ArrayDeque<>();
            HashSet<String> seen=new HashSet<>();
            queue.add(new Pending(rootId,"",0)); seen.add(rootId);
            try(BufferedWriter out=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(temp,false),StandardCharsets.UTF_8))) {
                while(!queue.isEmpty()) {
                    Pending node=queue.removeFirst();
                    if(node.depth>=InventorySafety.MAX_DEPTH){truncated=true;continue;}
                    Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,node.id);
                    try(Cursor cursor=getContentResolver().query(children,COLUMNS,null,null,null)) {
                        if(cursor==null){errors++;continue;}
                        while(cursor.moveToNext()) {
                            ++rowsSeen;
                            if(!InventorySafety.mayReadRow(rowsSeen) || !InventorySafety.mayEmit(records)){
                                truncated=true;queue.clear();break;
                            }
                            String id=cursor.getString(0), name=cursor.getString(1), mime=cursor.getString(2);
                            // Limits include malformed/duplicate rows, not just accepted objects.
                            if(id==null||name==null||name.length()>InventorySafety.MAX_NAME
                                    ||id.length()>InventorySafety.MAX_ID){errors++;continue;}
                            String path=node.path+"/"+name;
                            if(!InventorySafety.validEntry(id,name,mime,path)){errors++;continue;}
                            if(!seen.add(id)){repeatedIds++;continue;}
                            boolean dir=DocumentsContract.Document.MIME_TYPE_DIR.equals(mime);
                            long size=cursor.isNull(3)?-1:cursor.getLong(3);
                            long modified=cursor.isNull(4)?-1:cursor.getLong(4);
                            long traits=classifyName(name,dir);
                            if(traits<0){errors++;continue;}
                            int kind=(int)(traits&255L);
                            int prefix=(int)((traits>>8)&65535L);
                            int ext=(int)((traits>>24)&65535L);
                            String rec="{\"schema\":\"raf.atlas.object.v1\",\"path\":"+quote(path)
                                +",\"document_id\":"+quote(id)
                                +",\"parent_document_id\":"+quote(node.id)
                                +",\"mime\":"+quote(mime==null?"TOKEN_VAZIO_MIME":mime)
                                +",\"kind\":"+kind+",\"numeric_prefix_bytes\":"+prefix
                                +",\"extension_offset_bytes\":"+ext
                                +",\"size_bytes\":"+(size<0?"null":Long.toString(size))
                                +",\"modified_ms\":"+(modified<=0?"null":Long.toString(modified))
                                +",\"content_sha256\":null,\"semantic_verified\":false}";
                            out.write(rec);out.write('\n');
                            digest.update((rec+"\n").getBytes(StandardCharsets.UTF_8));
                            records++;
                            if(dir){dirs++;queue.add(new Pending(id,path,node.depth+1));}
                            else {
                                files++;
                                if(size>=0){
                                    if(Long.MAX_VALUE-bytes<size)overflow=true;
                                    else bytes+=size;
                                } else ++unknownSizes;
                            }
                        }
                    } catch(Exception e){errors++;}
                }
                String receipt="{\"schema\":\"raf.atlas.receipt.v1\",\"kind\":\"RECEIPT\""
                    +",\"records\":"+records+",\"files\":"+files+",\"directories\":"+dirs
                    +",\"rows_seen\":"+rowsSeen+",\"duplicate_document_ids\":"+repeatedIds
                    +",\"unknown_size_count\":"+unknownSizes+",\"root_document_id\":"+quote(rootId)
                    +",\"provider_authority\":"+quote(tree.getAuthority())
                    +",\"known_file_bytes\":"+(overflow?"null":Long.toString(bytes))
                    +",\"bytes_overflow\":"+overflow+",\"errors\":"+errors
                    +",\"truncated\":"+truncated+",\"complete\":false"
                    +",\"enumeration_finished\":"+(!truncated&&errors==0)
                    +",\"coverage_state\":"+quote(InventorySafety.coverageState(truncated,errors,repeatedIds))
                    +",\"scope\":\"USER_SELECTED_SAF_TREE\",\"source_bytes_verified\":false"
                    +",\"hash_scope\":\"UTF8_JSONL_ENTRIES_EXCLUDING_RECEIPT\""
                    +",\"entries_sha256\":\""+hex(digest.digest())+"\""
                    +",\"claim_allowed\":false,\"timestamp_ms\":"+System.currentTimeMillis()+"}";
                out.write(receipt);out.write('\n');
            }
            output=temp;
            show("Registros="+records+" arquivos="+files+" pastas="+dirs
                +" linhas="+rowsSeen+" repetidos="+repeatedIds+" tamanhos desconhecidos="+unknownSizes
                +" erros="+errors+" truncado="+truncated+".\nCobertura: "
                +InventorySafety.coverageState(truncated,errors,repeatedIds)
                +". Hashes da fonte TOKEN_VAZIO. Exportação explícita.",true);
        } catch(Exception e){
            output=null;
            show("Inventário bloqueado: "+e.getClass().getSimpleName()+"; recibo não confiável.",false);
        }
    }
    private void show(String message,boolean allowSave){
        runOnUiThread(()->{
            busy=false;status.setText(message);pick.setEnabled(true);
            save.setEnabled(allowSave&&output!=null);
        });
    }
    private static String quote(String s) {
        if(s==null)return "null";
        StringBuilder b=new StringBuilder("\"");
        for(int i=0;i<s.length();i++){
            char c=s.charAt(i);
            switch(c){
                case '"':b.append("\\\"");break;
                case '\\':b.append("\\\\");break;
                case '\n':b.append("\\n");break;
                case '\r':b.append("\\r");break;
                case '\t':b.append("\\t");break;
                default:
                    if(c<32){
                        final char[] h="0123456789abcdef".toCharArray();
                        b.append("\\u00").append(h[(c>>4)&15]).append(h[c&15]);
                    } else b.append(c);
            }
        }
        return b.append('"').toString();
    }
    private static String hex(byte[] bytes){
        final char[] d="0123456789abcdef".toCharArray();
        char[] out=new char[bytes.length*2];
        for(int i=0;i<bytes.length;i++){
            out[i*2]=d[(bytes[i]>>>4)&15];
            out[i*2+1]=d[bytes[i]&15];
        }
        return new String(out);
    }
}