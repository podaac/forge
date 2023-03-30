package gov.nasa.podaac.forge;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class FootprintBenchmark {

    private final static int NUM_WARMUP = 5;
    private final static int NUM_RUNS = 5;

    private static void runFootprint(String fileDirectory, String inputFileName, String configFileName) throws Exception{
        String ncFile = fileDirectory + "/" + inputFileName;
        String configFile = fileDirectory + "/" + configFileName;

        Footprinter footprinter = new Footprinter(ncFile, configFile);
        try{
            System.out.println("Processing File");
            Map<String,String> fp = footprinter.footprint();
            System.out.println("Process retrieved footprint");
            System.out.println(fp.get("FOOTPRINT"));
        }catch(Exception e){
            System.err.println(ncFile);
            e.printStackTrace(System.err);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = NUM_WARMUP)
    @Measurement(iterations = NUM_RUNS)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public static void footprintModisT() throws Exception {
        String fileDirectory = "src/test/resources/MODIS_T";
        String inputFileName = "20190101235501-JPL-L2P_GHRSST-SSTskin-MODIS_T-N-v02.0-fv01.0.nc";
        String configFileName = "PODAAC-GHMDT-2PJ19.cfg";
        runFootprint(fileDirectory, inputFileName, configFileName);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = NUM_WARMUP)
    @Measurement(iterations = NUM_RUNS)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public static void footprintModisA() throws Exception {
        String fileDirectory = "src/test/resources/MODIS_A";
        String inputFileName = "20190101235500-JPL-L2P_GHRSST-SSTskin-MODIS_A-D-v02.0-fv01.0.nc";
        String configFileName = "PODAAC-GHMDA-2PJ19.cfg";
        runFootprint(fileDirectory, inputFileName, configFileName);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = NUM_WARMUP)
    @Measurement(iterations = NUM_RUNS)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public static void footprintCYGNSS() throws Exception {
        String fileDirectory = "src/test/resources/CYGNSS";
        String inputFileName = "subsetted-cyg.ddmi.s20201031-000000-e20201031-235959.l2.wind-mss.a21.d21.nc";
        String configFileName = "PODAAC-CYGNS-L2X21.cfg";
        runFootprint(fileDirectory, inputFileName, configFileName);
    }

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(".*" + FootprintBenchmark.class.getSimpleName() + ".*")
                .forks(1)
                .build();
        new Runner(opt).run();
    }
}
