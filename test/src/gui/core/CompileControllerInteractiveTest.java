package src.gui.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CompileControllerInteractiveTest {

    @Test
    void interactiveRun_acceptsInputLineByLine() throws Exception {
        String source = """
                void main() {
                    int a;
                    int b;
                    read(a);
                    read(b);
                    print(a, b);
                }
                """;

        CompileController controller = new CompileController();
        List<String> chunks = new ArrayList<>();
        List<String> done = new ArrayList<>();

        controller.setRuntimeEventListener(new RuntimeEventListener() {
            @Override
            public void onRuntimeOutput(String text) {
                chunks.add(text);
            }

            @Override
            public void onRuntimeFinished(String finalOutput, String runtimeError) {
                if (runtimeError != null) {
                    done.add("ERR:" + runtimeError);
                } else {
                    done.add(finalOutput);
                }
            }
        });

        controller.compileInteractive(source);
        Thread.sleep(80);
        controller.submitRuntimeInputLine("10");
        Thread.sleep(80);
        controller.submitRuntimeInputLine("20");

        long deadline = System.currentTimeMillis() + 2000;
        while (done.isEmpty() && System.currentTimeMillis() < deadline) {
            Thread.sleep(25);
        }

        assertTrue(!done.isEmpty(), "interactive run should finish");
        assertTrue(done.get(0).contains("10 20"), "final output should include both input values");
    }
}
