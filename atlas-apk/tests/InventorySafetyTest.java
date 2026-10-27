/* SPDX-License-Identifier: GPL-3.0-or-later */
package org.rafaelia.atlas;
/** Offline negative fixtures, not evidence of a physical Android SAF run. */
public final class InventorySafetyTest {
  private static int checks;
  private static void ok(boolean b){if(!b)throw new AssertionError("fixture-"+(++checks)); ++checks;}
  public static void main(String[] args){
    ok(!InventorySafety.mayReadRow(0));ok(InventorySafety.mayReadRow(1));
    ok(InventorySafety.mayReadRow(InventorySafety.MAX_ROWS));
    ok(!InventorySafety.mayReadRow(InventorySafety.MAX_ROWS+1L));
    ok(InventorySafety.mayEmit(InventorySafety.MAX_OBJECTS-1));
    ok(!InventorySafety.mayEmit(InventorySafety.MAX_OBJECTS));ok(!InventorySafety.mayEmit(-1));
    ok(!InventorySafety.validRootId(null));ok(!InventorySafety.validRootId(""));
    ok(!InventorySafety.validRootId("x".repeat(InventorySafety.MAX_ID+1)));
    ok(InventorySafety.validRootId("root"));
    ok(!InventorySafety.validEntry(null,"f","text/plain","/f"));
    ok(!InventorySafety.validEntry("id","","text/plain","/f"));
    ok(!InventorySafety.validEntry("id","x".repeat(InventorySafety.MAX_NAME+1),"text/plain","/x"));
    ok(!InventorySafety.validEntry("id","f","m".repeat(InventorySafety.MAX_MIME+1),"/f"));
    ok(!InventorySafety.validEntry("id","f","text/plain","x".repeat(InventorySafety.MAX_PATH+1)));
    ok(InventorySafety.validEntry("id","f",null,"/f"));
    ok(InventorySafety.coverageState(false,0,0).equals("PROVIDER_ENUMERATED_UNVERIFIED"));
    ok(InventorySafety.coverageState(true,0,0).equals("PARTIAL_OR_AMBIGUOUS"));
    ok(InventorySafety.coverageState(false,1,0).equals("PARTIAL_OR_AMBIGUOUS"));
    ok(InventorySafety.coverageState(false,0,1).equals("PARTIAL_OR_AMBIGUOUS"));
    System.out.println("INVENTORY_SAFETY_HOST_PASS checks="+checks);
  }
}
