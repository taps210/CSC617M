// C syntax comparison for LexRecovery_Multiple.txt
// Intentionally includes the same "bad" tokens to show how C reports them.
// (This file is for presentation/comparison; it is not part of the Java test suite.)

#include <stdio.h>

int main(void)
{
    // In C, this is a compile-time lexical error: invalid suffix "a" on integer constant.
    int x = 12a + 3;

    // In C, single '|' is a valid bitwise OR operator (unlike the .hd language which expects '||').
    int y = 4 | 5;

    // In C, '#' is only valid for preprocessor directives, not as an infix operator.
    int z = 6 #7;

    printf("%d %d %d\n", x, y, z);
    return 0;
}
