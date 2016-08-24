package org.cellocad.MIT.dnacompiler;


public class CelloMain {


    /**
     * Cello main: this is the function that is called to run Cello
     *
     * @param args
     */
    public static void main(String[] args) {

        //-fin_verilog SR_latch.v -circuit_type sequential -synthesis originalstructural -waveform SR_latch_waveform.txt
        String workingDir = "/home/arashkh/Documents/CIDAR/LCP/logicDesign/cello/resources/verilog/sequential/SR_latch/";
        //String workingDir = "";
        String fin_verilog = workingDir + "SR_latch.v";
        String waveform = workingDir + "SR_latch_waveform.txt";
        String[] argsHard = {"-fin_verilog", fin_verilog, "-circuit_type", "sequential", "-synthesis", "originalstructural", "-waveform", waveform};
        DNACompiler dnaCompiler = new DNACompiler();
        dnaCompiler.run(argsHard);

    }
}
