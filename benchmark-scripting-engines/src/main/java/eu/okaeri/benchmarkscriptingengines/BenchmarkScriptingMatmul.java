package eu.okaeri.benchmarkscriptingengines;

import groovy.lang.GroovyClassLoader;
import lombok.SneakyThrows;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;
import java.lang.reflect.Method;

@Warmup(iterations = 3)
@Measurement(iterations = 3)
@Fork(value = 1, warmups = 1)
@Timeout(time = 10)
@BenchmarkMode(Mode.Throughput)
public class BenchmarkScriptingMatmul {

    static {
        System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");
    }

    public static void main(String[] args) throws IOException {
        org.openjdk.jmh.Main.main(args);
    }

    @State(Scope.Benchmark)
    public static class GraaljsHelper {

        public Value main;

        @SneakyThrows
        public GraaljsHelper() {
            String script = "const matgen = (n) => {\n" +
                    "\n" +
                    "    let a = []\n" +
                    "    let tmp = 1.0 / n / n\n" +
                    "\n" +
                    "    for (let i = 0; i < n; ++i) {\n" +
                    "        a[i] = []\n" +
                    "        for (let j = 0; j < n; ++j) {\n" +
                    "            a[i][j] = tmp * (i - j) * (i + j)\n" +
                    "        }\n" +
                    "    }\n" +
                    "    \n" +
                    "    return a\n" +
                    "};\n" +
                    "\n" +
                    "const matmul = (a, b) => {\n" +
                    "\n" +
                    "    const m = a.length\n" +
                    "    const n = a[0].length\n" +
                    "    const p = b[0].length\n" +
                    "\n" +
                    "    let x = []\n" +
                    "    let c = []\n" +
                    "\n" +
                    "    for (let i = 0; i < p; ++i) {\n" +
                    "        c[i] = []\n" +
                    "        for (let j = 0; j < n; ++j) {\n" +
                    "            c[i][j] = b[j][i];\n" +
                    "        }\n" +
                    "    }\n" +
                    "\n" +
                    "    for (let i = 0; i < m; ++i) {\n" +
                    "        x[i] = []\n" +
                    "        for (let j = 0; j < p; ++j) {\n" +
                    "\n" +
                    "            let s = 0.0\n" +
                    "            for (let k = 0; k < n; ++k) {\n" +
                    "                s = s + a[i][k] * c[j][k]\n" +
                    "            }\n" +
                    "\n" +
                    "            x[i][j] = s\n" +
                    "        }\n" +
                    "    }\n" +
                    "\n" +
                    "    return x\n" +
                    "};\n" +
                    "\n" +
                    "const main = () => {\n" +
                    "    const n = 100\n" +
                    "    const a = matgen(n)\n" +
                    "    const b = matgen(n)\n" +
                    "    const x = matmul(a, b)\n" +
                    "    return x[n / 2][n / 2]\n" +
                    "};\n";

            Context context = Context.create();
            context.eval(Source.newBuilder("js", script, "matmul.js").build());
            main = context.getBindings("js").getMember("main");
        }
    }

    @Benchmark
    public void graaljs_matmul(GraaljsHelper helper, Blackhole blackhole) {
        Value result = helper.main.execute();
        blackhole.consume(result);
    }

    @State(Scope.Benchmark)
    public static class GroovyHelper {

        public Method main;

        @SneakyThrows
        public GroovyHelper() {
            String script = "static def matgen(def n) {\n" +
                    "\n" +
                    "    def a = []\n" +
                    "    def tmp = 1.0 / n / n\n" +
                    "\n" +
                    "    for (def i = 0; i < n; ++i) {\n" +
                    "        a[i] = []\n" +
                    "        for (def j = 0; j < n; ++j) {\n" +
                    "            a[i][j] = tmp * (i - j) * (i + j)\n" +
                    "        }\n" +
                    "    }\n" +
                    "    \n" +
                    "    return a\n" +
                    "}\n" +
                    "\n" +
                    "static def matmul(def a, def b) {\n" +
                    "\n" +
                    "    def m = a.size()\n" +
                    "    def n = a[0].size()\n" +
                    "    def p = b[0].size()\n" +
                    "\n" +
                    "    def x = []\n" +
                    "    def c = []\n" +
                    "\n" +
                    "    for (def i = 0; i < p; ++i) {\n" +
                    "        c[i] = []\n" +
                    "        for (def j = 0; j < n; ++j) {\n" +
                    "            c[i][j] = b[j][i];\n" +
                    "        }\n" +
                    "    }\n" +
                    "\n" +
                    "    for (def i = 0; i < m; ++i) {\n" +
                    "        x[i] = []\n" +
                    "        for (def j = 0; j < p; ++j) {\n" +
                    "\n" +
                    "            def s = 0.0\n" +
                    "            for (def k = 0; k < n; ++k) {\n" +
                    "                s = s + a[i][k] * c[j][k]\n" +
                    "            }\n" +
                    "\n" +
                    "            x[i][j] = s\n" +
                    "        }\n" +
                    "    }\n" +
                    "\n" +
                    "    return x\n" +
                    "}\n" +
                    "\n" +
                    "static double main() {\n" +
                    "    def n = 100\n" +
                    "    def a = matgen(n)\n" +
                    "    def b = matgen(n)\n" +
                    "    def x = matmul(a, b)\n" +
                    "    return x[n / 2][n / 2]\n" +
                    "}\n";

            Class<?> gClass = new GroovyClassLoader().parseClass(script);
            this.main = gClass.getDeclaredMethod("main");
        }
    }

    @Benchmark
    @SneakyThrows
    public void groovy_matmul(GroovyHelper helper, Blackhole blackhole) {
        Object result = helper.main.invoke(null);
        blackhole.consume(result);
    }

    @State(Scope.Benchmark)
    public static class GroovyCSHelper {

        public Method main;

        @SneakyThrows
        public GroovyCSHelper() {
            String script = "import groovy.transform.CompileStatic\n" +
                    "\n" +
                    "@CompileStatic\n" +
                    "static double[][] matgen(int n) {\n" +
                    "\n" +
                    "    double[][] a = new double[n][n];\n" +
                    "    double tmp = 1.0 / n / n\n" +
                    "\n" +
                    "    for (int i = 0; i < n; ++i) {\n" +
                    "        for (int j = 0; j < n; ++j) {\n" +
                    "            a[i][j] = tmp * (i - j) * (i + j)\n" +
                    "        }\n" +
                    "    }\n" +
                    "\n" +
                    "    return a\n" +
                    "}\n" +
                    "\n" +
                    "@CompileStatic\n" +
                    "static double[][] matmul(double[][] a, double[][] b) {\n" +
                    "\n" +
                    "    int m = a.length\n" +
                    "    int n = a[0].length\n" +
                    "    int p = b[0].length\n" +
                    "\n" +
                    "    double[][] x = new double[m][p]\n" +
                    "    double[][] c = new double[p][n]\n" +
                    "\n" +
                    "    for (int i = 0; i < p; ++i) {\n" +
                    "        for (int j = 0; j < n; ++j) {\n" +
                    "            c[i][j] = b[j][i];\n" +
                    "        }\n" +
                    "    }\n" +
                    "\n" +
                    "    for (int i = 0; i < m; ++i) {\n" +
                    "        for (int j = 0; j < p; ++j) {\n" +
                    "\n" +
                    "            double s = 0.0\n" +
                    "            for (int k = 0; k < n; ++k) {\n" +
                    "                s = s + a[i][k] * c[j][k]\n" +
                    "            }\n" +
                    "\n" +
                    "            x[i][j] = s\n" +
                    "        }\n" +
                    "    }\n" +
                    "\n" +
                    "    return x\n" +
                    "}\n" +
                    "\n" +
                    "@CompileStatic\n" +
                    "static def main() {\n" +
                    "    int n = 100\n" +
                    "    double[][] a = matgen(n)\n" +
                    "    double[][] b = matgen(n)\n" +
                    "    double[][] x = matmul(a, b)\n" +
                    "    return x[(n / 2).intValue()][(n / 2).intValue()]\n" +
                    "}\n";

            Class<?> gClass = new GroovyClassLoader().parseClass(script);
            this.main = gClass.getDeclaredMethod("main");
        }
    }

    @Benchmark
    @SneakyThrows
    public void groovy_matmul_compile_static(GroovyCSHelper helper, Blackhole blackhole) {
        Object result = helper.main.invoke(null);
        blackhole.consume(result);
    }
}
