package src;

import org.junit.jupiter.api.Test;

/**
 * Tests derived from herd_cfg_reference.md. Each test corresponds to a CFG rule or
 * invalid example from that document. This suite ensures parser coverage is traceable to the grammar.
 */
class ParserCfgCoverageTest extends ParserTestBase {

    // ---------- §2 Use statement ----------
    @Test
    void use_stmt_multiPartFilename_success() {
        assertParseSuccess("use std.math.utils; void main() { }");
    }

    // ---------- §3 Type alias / record ----------
    @Test
    void type_alias_emptyRecord_success() {
        assertParseSuccess("type Empty = record { }; void main() { }");
    }

    // ---------- §3 World ----------
    @Test
    void world_emptyWorld_success() {
        assertParseSuccess("world Empty { } void main() { }");
    }

    @Test
    void world_missingIdent_error() {
        parseExpectError("world { } void main() { }", "Expected world name", 1, 7);
    }

    // ---------- §3 Agent ----------
    @Test
    void agent_missingUpdate_error() {
        parseExpectError("agent Ant { } void main() { }", "Expected 'update'", 1, 13);
    }

    @Test
    void agent_missingIdent_error() {
        parseExpectError("agent { int x; update { } } void main() { }", "Expected agent name", 1, 7);
    }

    // ---------- §3 Zone ----------
    @Test
    void zone_explicitSuccess_success() {
        assertParseSuccess("agent A { int temp; zone hotspot(temp > 30, grid) { step(1); } update { } } void main() { }");
    }

    @Test
    void zone_missingName_error() {
        parseExpectError("agent A { zone (2, Foo) { } update { } } void main() { }", "Expected zone name", 1, 16);
    }

    @Test
    void zone_missingSecondArg_error() {
        parseExpectError("agent A { zone hot(2) { } update { } } void main() { }", "Expected ','", 1, 21);
    }

    // ---------- §4 Const ----------
    @Test
    void const_allLiteralTypes_success() {
        assertParseSuccess("const MAX = 100; const PI = 3.14; const FLAG = true; const LETTER = 'z'; const MSG = \"hello\"; void main() { }");
    }

    @Test
    void const_missingSemicolon_error() {
        parseExpectError("const X = 5 void main() { }", "Expected ';'", 1, 13);
    }

    // ---------- §4 Variable declaration ----------
    @Test
    void var_decl_multiDeclarator_success() {
        assertParseSuccess("void main() { int a = 1, b = 2, c; }");
    }

    @Test
    void var_decl_pointerNull_success() {
        assertParseSuccess("bool* ptr = null; void main() { }");
    }

    /** CFG §5: pointer_suffix → * pointer_suffix | ε — valid types include float**, MyAgent* */
    @Test
    void var_decl_multiStarPointer_success() {
        assertParseSuccess("float** pp = null; void main() { }");
    }

    @Test
    void var_decl_matrix_success() {
        assertParseSuccess("void main() { int m[3][4]; }");
    }

    // ---------- §6 Function declaration ----------
    @Test
    void func_decl_nonVoidReturnAndParams_success() {
        assertParseSuccess("int add(int a, int b) { return a + b; } void main() { }");
    }

    @Test
    void func_decl_paramWithArrayDims_success() {
        assertParseSuccess("void f(int arr[10]) { } void main() { }");
    }

    /** CFG §6: pointer return type and pointer parameter — float* getPtr(MyRecord r, int n) { return null; } */
    @Test
    void func_decl_pointerReturnAndPointerParam_success() {
        assertParseSuccess("float* getPtr(int* p) { return null; } void main() { }");
    }

    @Test
    void func_decl_missingName_error() {
        parseExpectError("int () { } void main() { }", "Expected variable/field name", 1, 5);
    }

    @Test
    void func_decl_missingParens_error() {
        parseExpectError("int foo { } void main() { }", "Expected ';'", 1, 9);
    }

    // ---------- §7 Main function ----------
    @Test
    void main_withParams_error() {
        parseExpectError("void main(int x) { }", "Expected ')'", 1, 11);
    }

    @Test
    void main_wrongReturnType_error() {
        parseExpectError("int main() { }", "Expected variable/field name", 1, 5);
    }

    // ---------- §8 Call statement ----------
    /** CFG §8 call_stmt: IDENT ( arg_list_opt ) */
    @Test
    void call_stmt_success() {
        assertParseSuccess("void doSomething() { } void main() { doSomething(); }");
    }

    @Test
    void call_stmt_withArgs_success() {
        assertParseSuccess("void compute(int a, int b) { } void main() { int x; int y; compute(x, y + 1); }");
    }

    /** CFG invalid: doSomething(; — missing closing paren (parser reports "expression atom" at ';') */
    @Test
    void call_stmt_missingClosingParen_error() {
        parseExpectError("void f() { } void main() { f(; }", "Expected expression atom", 1, 30);
    }

    /** CFG invalid: doSomething(,x); — leading comma */
    @Test
    void call_stmt_leadingComma_error() {
        parseExpectError("void f() { } void main() { int x; f(, x); }", "Expected expression atom", 1, 37);
    }

    // ---------- §8 Unclosed parenthesis / brace (easy to miss) ----------
    /** if ( expr ) statement — missing closing paren after condition */
    @Test
    void if_missingClosingParen_error() {
        parseExpectError("void main() { if (true { } }", "Expected ')'", 1, 24);
    }

    /** Block missing closing brace */
    @Test
    void block_missingClosingBrace_error() {
        parseExpectError("void main() { int x;", "Expected '}'", 1, 21);
    }

    /** CFG invalid: for (int i = 0; i < 10 i = i + 1) {} — missing second semicolon */
    @Test
    void for_missingSecondSemicolon_error() {
        parseExpectError("void main() { for (int i = 0; i < 10 i = i + 1) { } }", "Expected ';'", 1, 38);
    }

    // ---------- §8 Assignment ----------
    @Test
    void assign_derefPtr_success() {
        assertParseSuccess("int* p = null; void main() { *p = 99; }");
    }

    @Test
    void assign_field_success() {
        assertParseSuccess("type T = record { int x; }; void main() { T t; t.x = 3; }");
    }

    @Test
    void assign_matrixIndex_success() {
        assertParseSuccess("void main() { int m[2][2]; int i; int j; m[i][j] = 0; }");
    }

    // ---------- §8 FOR ----------
    @Test
    void for_emptyPartsAndAssignList_success() {
        assertParseSuccess("void main() { int x = 0; for (; x < 5; x = x + 1) { } }");
    }

    @Test
    void for_assignListInitAndUpdate_success() {
        assertParseSuccess("void main() { int x; int y; for (x = 0, y = 0; x < 10; x = x + 1, y = y + 1) { } }");
    }

    @Test
    void for_missingFirstSemicolon_error() {
        parseExpectError("void main() { for (int i = 0 i < 10; i = i + 1) { } }", "Expected ';'", 1, 30);
    }

    @Test
    void for_missingParens_error() {
        parseExpectError("void main() { for int i = 0; i < 10; i = i + 1 { } }", "Expected '('", 1, 19);
    }

    // ---------- §8 IF ----------
    @Test
    void if_missingParens_error() {
        parseExpectError("void main() { if x > 0 { } }", "Expected '('", 1, 18);
    }

    // ---------- §8 WHILE ----------
    @Test
    void while_missingParens_error() {
        parseExpectError("void main() { while x > 0 { } }", "Expected '('", 1, 21);
    }

    // ---------- §8 REPEAT-UNTIL ----------
    @Test
    void repeat_bodyMustBeBlock_error() {
        parseExpectError("void main() { repeat x = 1; until (x); }", "Expected '{'", 1, 22);
    }

    @Test
    void repeat_missingParensAroundCondition_error() {
        parseExpectError("void main() { repeat { } until x >= 10; }", "Expected '('", 1, 32);
    }

    @Test
    void repeat_missingTrailingSemicolon_error() {
        parseExpectError("void main() { repeat { } until (true) }", "Expected ';'", 1, 39);
    }

    // ---------- §8 RETURN ----------
    @Test
    void return_voidAndExpr_success() {
        assertParseSuccess("void f() { return; } int g() { return 42; } void main() { }");
    }

    // ---------- §8 ASSERT ----------
    @Test
    void assert_missingParens_error() {
        parseExpectError("void main() { assert x > 0; }", "Expected '('", 1, 22);
    }

    // ---------- §8 READ / PRINT ----------
    @Test
    void read_printVariants_success() {
        assertParseSuccess("void main() { int arr[10]; int x; read(arr[0]); print(); print(x); print(x, x, \"done\"); }");
    }

    /** CFG §8 read(lvalue): lvalue can be *ptr (dereference). */
    @Test
    void read_derefPtrLvalue_success() {
        assertParseSuccess("void main() { int x; int* p; p = &x; read(*p); }");
    }

    @Test
    void read_missingLvalue_error() {
        parseExpectError("void main() { read(); }", "Expected identifier", 1, 20);
    }

    @Test
    void read_exprNotLvalue_error() {
        parseExpectError("void main() { int x; read(x + 1); }", "Expected ')'", 1, 29);
    }

    // ---------- §9 ABM SPAWN ----------
    @Test
    void spawn_noArgsAndMultiArg_success() {
        assertParseSuccess("agent Ant { update { } } agent Bee { int x; int y; float e; update { } } void main() { spawn Ant(); spawn Bee(0, 0, 1.0); }");
    }

    @Test
    void spawn_missingType_error() {
        parseExpectError("agent A { update { } } void main() { spawn (); }", "Expected agent type", 1, 44);
    }

    @Test
    void spawn_missingParens_error() {
        parseExpectError("agent A { update { } } void main() { spawn A; }", "Expected '('", 1, 45);
    }

    // ---------- §9 ABM MOVE ----------
    @Test
    void move_2dAnd3d_success() {
        assertParseSuccess("agent A { int x; int y; int z; update { move(x + 1, y); move(x, y, z); } } void main() { }");
    }

    @Test
    void move_tooFewArgs_error() {
        parseExpectError("agent A { int x; update { move(x); } } void main() { }", "Expected ','", 1, 33);
    }

    @Test
    void move_missingParens_error() {
        parseExpectError("agent A { int x; int y; update { move x, y; } } void main() { }", "Expected '('", 1, 39);
    }

    // ---------- §9 ABM STEP ----------
    @Test
    void step_withArg_success() {
        assertParseSuccess("void main() { step(1); }");
    }

    @Test
    void step_missingParens_error() {
        parseExpectError("void main() { step; }", "Expected '('", 1, 19);
    }

    // ---------- §9 ABM DESTROY ----------
    @Test
    void destroy_selfAndRef_success() {
        assertParseSuccess("agent A { update { destroy(self); } } void main() { }");
    }

    @Test
    void destroy_missingExpr_error() {
        parseExpectError("agent A { update { destroy(); } } void main() { }", "Expected expression atom", 1, 28);
    }

    @Test
    void destroy_missingParens_error() {
        parseExpectError("agent A { update { destroy self; } } void main() { }", "Expected '('", 1, 28);
    }

    // ---------- §10 Expressions ----------
    @Test
    void expr_addressOfAndDeref_success() {
        assertParseSuccess("void main() { int x; int* p; p = &x; *p = 0; }");
    }

    @Test
    void expr_selfField_success() {
        assertParseSuccess("agent A { int energy; update { self.energy = 0; move(self.energy, 0); } } void main() { }");
    }

    @Test
    void expr_nullLiteral_success() {
        assertParseSuccess("int* p = null; void main() { if (p == null) { } }");
    }

    /** CFG §8 assert(expr): assert(ptr != null) — pointer in condition. */
    @Test
    void assert_ptrNotNull_success() {
        assertParseSuccess("int* ptr = null; void main() { assert(ptr != null); }");
    }
}
