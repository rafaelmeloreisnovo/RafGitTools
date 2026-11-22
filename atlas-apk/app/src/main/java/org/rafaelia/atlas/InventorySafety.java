/* SPDX-License-Identifier: GPL-3.0-or-later */
package org.rafaelia.atlas;
/** Pure-Java provider-input limits: no Android/network API. */
final class InventorySafety {
  static final int MAX_OBJECTS=5000, MAX_ROWS=6000, MAX_DEPTH=16, MAX_PATH=2048;
  static final int MAX_ID=1024, MAX_NAME=512, MAX_MIME=256;
  private InventorySafety(){}
  static boolean mayReadRow(long rowsSeen){return rowsSeen>0 && rowsSeen<=MAX_ROWS;}
  static boolean mayEmit(int records){return records>=0 && records<MAX_OBJECTS;}
  /* Missing MIME cannot prove that the entry is not a directory. */
  static boolean knownMime(String mime){return mime!=null && !mime.isEmpty();}
  static boolean validRootId(String id){return id!=null && !id.isEmpty() && id.length()<=MAX_ID;}
  static boolean validEntry(String id,String name,String mime,String path){
    return validRootId(id) && name!=null && !name.isEmpty() && name.length()<=MAX_NAME
      && (mime==null || mime.length()<=MAX_MIME) && path!=null && path.length()<=MAX_PATH;
  }
  /* Enumerated provider rows are not proof of source coverage. */
  static String coverageState(boolean truncated,int errors,int repeatedIds,int unknownMimes){
    return truncated || errors>0 || repeatedIds>0 || unknownMimes>0
      ? "PARTIAL_OR_AMBIGUOUS" : "PROVIDER_ENUMERATED_UNVERIFIED";
  }
}
