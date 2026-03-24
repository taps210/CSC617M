#!/usr/bin/env bash
# describe_pointers.sh — Overview of Herd pointer types implementation

BOLD="\033[1m"
CYAN="\033[36m"
YELLOW="\033[33m"
GREEN="\033[32m"
RED="\033[31m"
DIM="\033[2m"
RESET="\033[0m"

section() { echo -e "\n${BOLD}${CYAN}══ $1 ══${RESET}"; }
sub()     { echo -e "\n${BOLD}${YELLOW}-- $1 --${RESET}"; }
item()    { echo -e "  ${GREEN}•${RESET} $1"; }
code()    { echo -e "  ${DIM}$1${RESET}"; }
warn()    { echo -e "  ${RED}!${RESET} $1"; }

echo -e "${BOLD}Herd Language — Pointer Types Implementation${RESET}"
echo -e "${DIM}Branch: Semantic | Language: Herd (.hd)${RESET}"

# ─────────────────────────────────────────────
section "WHAT WE SUPPORT"
# ─────────────────────────────────────────────

sub "Syntax"
item "Pointer declaration:     Drop* ptr;"
item "Heap allocation:         ptr = new Drop();"
item "Field write:             ptr->x = 5;"
item "Field read:              int v = ptr->x;"
item "Pointer copy (alias):    Drop* ptr2 = ptr;   // refCount++"
item "Null assignment:         ptr = null;          // refCount--; GC if 0"

sub "What is NOT supported (v1 scope)"
warn "Address-of operator:     &variable"
warn "Pointer arithmetic:      ptr + 1"
warn "Arrays of pointers:      Drop* arr[]"
warn "Constructor arguments:   new Drop(arg)   // fields always default to 0/null"
warn "Cycle-safe GC:           circular references will leak (see Known Limitations)"

sub "Type safety"
item "Drop* is NOT compatible with Forager* — same pointer level, different base type"
item "Drop* is NOT compatible with Drop[]   — pointer vs. array distinction tracked by DataTypeNode.isPointer"
item "null is assignable to any pointer type"
item "'->' on a non-pointer type is a semantic error; '.' on a pointer type is also an error"

# ─────────────────────────────────────────────
section "HOW IT IS IMPLEMENTED"
# ─────────────────────────────────────────────

sub "1. Token & Scanner  (TokenType.java, Scanner.java)"
item "Two new tokens added: NEW (keyword), ARROW (operator '->')"
item "Scanner: 'new' added to keyword map"
item "Scanner: '-' case peeks ahead — if next char is '>', emits ARROW; else MINUS"

sub "2. AST  (Ast.java)"
item "DataTypeNode gains a 4th field: boolean isPointer"
item "  3-arg constructor kept for backward-compat (isPointer defaults to false)"
item "New AST node: NewExprNode(location, typeName)"
item "  -> expression becomes: BinaryExprNode(location, left, \"->\", fieldIdentExprNode)"

sub "3. Parser  (Parser.java)"
item "atom() recognizes: new TypeName()  ->  ParseTreeKind.NEW_EXPR"
item "primary() postfix chain: ARROW consumed like DOT, yields '->' binary node"
item "lvalueTail() also accepts ARROW so 'ptr->field = val' works as an lvalue"
code "  lvalue:  ptr->x = 5   parsed as  BinaryExprNode(\"->\", ptr, x)"

sub "4. Semantic Analysis  (SemanticAnalyzer.java)"
item "NewExprNode:  resolves typeName in symbol table; must be an agent type"
item "  returns:    DataTypeNode(typeName, pointerLevel=1, isPointer=true)"
item "BinaryExprNode(\"->\"):  checks left side has isPointer=true"
item "  resolves field name against the agent's declared fields"
item "typesCompatible():  both sides must have same baseTypeName AND same isPointer flag"

sub "5. IR Instructions  (Instr.java)"
item "HeapAllocInstr(result, typeName)   — allocate heap object"
item "HeapLoadInstr(result, ptr, field)  — read field through pointer"
item "HeapStoreInstr(ptr, field, value)  — write field through pointer"
item "IrBuilder emits these when it encounters NewExprNode or '->' expressions"
code "  IR text:  t0 = new Drop"
code "  IR text:  t1 = (*t0)->x"
code "  IR text:  (*t0)->x = t1"

sub "6. Heap + Reference Counting  (IrInterpreter.java)"
item "Heap:    Map<Integer, HeapObject>  keyed by object ID (starts at 1)"
item "HeapPointer(objectId) — wrapper stored in the variable store; never a raw int"
item "HeapObject: { id, typeName, Map<String,Object> fields, int refCount, boolean freed }"
echo ""
item "HeapAllocInstr:"
code "    obj = new HeapObject(nextObjectId++, typeName)"
code "    initDefaultFields(obj)   // scans AST for agent field declarations"
code "    heap.put(obj.id, obj)"
code "    store[result] = new HeapPointer(obj.id)"
echo ""
item "HeapLoadInstr:"
code "    ptr  = (HeapPointer) store[h.ptr]"
code "    if ptr == null  -> RuntimeException: Null pointer dereference"
code "    if obj.freed    -> RuntimeException: Use-after-free"
code "    store[result]   = obj.fields[fieldName]  (default 0 if missing)"
echo ""
item "HeapStoreInstr:"
code "    ptr = (HeapPointer) store[h.ptr]"
code "    null/freed checks same as above"
code "    oldVal = obj.fields[fieldName]"
code "    if oldVal is HeapPointer -> decRef(oldVal.objectId)"
code "    if newVal is HeapPointer -> incRef(newVal.objectId)"
code "    obj.fields[fieldName] = newVal"

sub "6a. storeSet() — every variable assignment"
item "All store writes go through storeSet() instead of store.put()"
code "    storeSet(name, newVal):"
code "      if store[name] is HeapPointer -> decRef(old.objectId)"
code "      if newVal      is HeapPointer -> incRef(new.objectId)"
code "      store.put(name, newVal)"
item "This fires on: local var assignment, function arg binding, loop var update, etc."

sub "6b. incRef / decRef"
item "incRef(id):  obj.refCount++"
item "decRef(id):  obj.refCount--"
code "    if refCount <= 0:"
code "      obj.freed = true"
code "      for each field value that is HeapPointer -> decRef(nested)"
code "      heap.remove(id)"
item "Recursive decRef walks the object graph on free — no leaks for non-cyclic structures"

sub "6c. initDefaultFields() — AST-driven initialization"
item "Scans program.typeDecls() to find the matching AgentDeclNode"
item "For each declared field: initializes to 0 (int/float), false (bool), \"\" (string), null (pointer)"
item "No separate metadata cache needed — reuses the existing AST"

sub "7. IrOptimizer  (IrOptimizer.java)"
item "HeapLoadInstr  is PURE  — can be eliminated if result is unused"
item "HeapAllocInstr is SIDE-EFFECTFUL — never eliminated (allocates an object)"
item "HeapStoreInstr is SIDE-EFFECTFUL — never eliminated (mutates heap state)"
item "usedSymbols() and definedSymbol() updated for all three instructions"
item "HeapStoreInstr pessimistically clears all knownConstants on constant folding pass"

sub "8. Debugger Display  (DebuggerPanel.java)"
item "HeapPointer values displayed as '#1', '#2', etc. in the variables pane"
item "Lets you visually track which variable aliases which heap object"

# ─────────────────────────────────────────────
section "KNOWN LIMITATIONS"
# ─────────────────────────────────────────────

warn "Circular references LEAK — reference counting cannot detect cycles"
warn "Example:"
code "    agent A { B* neighbor; }"
code "    agent B { A* neighbor; }"
code "    A* a = new A();  B* b = new B();"
code "    a->neighbor = b;  // b.refCount = 2"
code "    b->neighbor = a;  // a.refCount = 2"
code "    a = null;         // a.refCount = 1 — object NOT freed"
code "    b = null;         // b.refCount = 1 — object NOT freed  =>  LEAK"
warn "Workaround: avoid circular pointer graphs in v1"
warn "Future fix: mark-and-sweep GC as a v2 addition"

# ─────────────────────────────────────────────
section "FILES CHANGED"
# ─────────────────────────────────────────────

printf "  %-50s %s\n" "src/src/TokenType.java"                  "Add NEW, ARROW"
printf "  %-50s %s\n" "src/src/Scanner.java"                    "Tokenize 'new', '->'"
printf "  %-50s %s\n" "src/src/Ast.java"                        "NewExprNode; DataTypeNode.isPointer"
printf "  %-50s %s\n" "src/src/parsetree/ParseTreeKind.java"    "NEW_EXPR"
printf "  %-50s %s\n" "src/src/Parser.java"                     "new Type(), -> postfix, lvalue"
printf "  %-50s %s\n" "src/src/parsetree/ParseTreeToAst.java"   "NEW_EXPR -> NewExprNode; ARROW -> BinaryExprNode"
printf "  %-50s %s\n" "src/src/semantic/SemanticAnalyzer.java"  "Type-check new, ->, pointer compat"
printf "  %-50s %s\n" "src/src/ir/Instr.java"                   "HeapAllocInstr, HeapLoadInstr, HeapStoreInstr"
printf "  %-50s %s\n" "src/src/ir/IrBuilder.java"               "Emit heap instructions"
printf "  %-50s %s\n" "src/src/ir/IrFormatter.java"             "Display heap instructions"
printf "  %-50s %s\n" "src/src/ir/IrOptimizer.java"             "Side-effect marking"
printf "  %-50s %s\n" "src/src/ir/IrInterpreter.java"           "Heap map, refcounting, storeSet"
printf "  %-50s %s\n" "src/gui/debug/DebuggerPanel.java"        "HeapPointer display as #id"

echo ""
