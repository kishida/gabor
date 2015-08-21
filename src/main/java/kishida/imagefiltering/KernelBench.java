/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kishida.imagefiltering;

import java.util.Random;

/**
 *
 * @author naoki
 */
public class KernelBench {
    static ConvolutionalNet.ConvolutionForwardKernel fKernel = new ConvolutionalNet.ConvolutionForwardKernel();
    static ConvolutionalNet.ConvolutionBackwordKernel bKernel = new ConvolutionalNet.ConvolutionBackwordKernel();
    static ConvolutionalNet.ConvolutionBackwordDeltaKernel bdKernel = new ConvolutionalNet.ConvolutionBackwordDeltaKernel();
    static ConvolutionalNet.ConvolutionBackwordFilterKernel bfKernel = new ConvolutionalNet.ConvolutionBackwordFilterKernel();
    static ConvolutionalNet.ConvolutionBackwordBiasKernel bbKernel = new ConvolutionalNet.ConvolutionBackwordBiasKernel();
    static ConvolutionalNet.NormalizeKernel nKernel = new ConvolutionalNet.NormalizeKernel();
    public static void main(String[] args) {
        Random r = new Random();
        
        double[] input = r.doubles(3 * 256 * 256).toArray();
        double[] filter = r.doubles(48 * 3 * 11 * 11).toArray();
        double[] bias = r.doubles(48).toArray();
        double[] result = r.doubles(48 * 128 * 128).toArray();
        double[] delta = r.doubles(result.length).toArray();
        double[] input2 = r.doubles(48 * 128 * 128).toArray();
        double[] filter2 = r.doubles(96 * 48 * 5 * 5).toArray();
        double[] bias2 = r.doubles(96).toArray();
        double[] result2 = r.doubles(96 * 16 * 16).toArray();
        double[] delta2 = r.doubles(result2.length).toArray();
        
        ConvolutionalNet.RetifierdLinear act = new ConvolutionalNet.RetifierdLinear();
        double[] averages = new double[48 * 32 * 32];
        double[] rates = new double[averages.length];
        bench("normalize gpu", () -> 
            nKernel.normalize(input, 48, 32, 32, 5, averages, rates, .1, true));
        bench("normalize cpu", () -> 
            nKernel.normalize(input, 48, 32, 32, 5, averages, rates, .1, false));
        
        bench("complex optimize gpu", () -> {
            fKernel.forward(input, 3, 256, 256, filter, 48, 256 / 2, 256 / 2, 11, 2, bias, act, true);
            fKernel.forward(input2, 48, 32, 32, filter2, 96, 16, 16, 5, 2, bias2, act, true);
            
            bdKernel.backword(input2, delta2, result2, 48, 32, 32, filter2, 96, 16, 16, 5, 2, true);
            bfKernel.backword(delta2, result2, input2, 48, 32, 32, filter2, 96, 16, 16, 5, 2, true);
            bbKernel.backwordBias(delta2, result2, 96, 16, 16, bias2, true);
            
            bdKernel.backword(input, delta, result, 3, 256, 256, filter, 48, 128, 128, 11, 2, true);
            bfKernel.backword(delta, result, input, 3, 256, 256, filter, 48, 128, 128, 11, 2, true);
            bbKernel.backwordBias(delta, result, 48, 128, 128, bias, true);
        });
        bench("complex gpu", () -> {
            fKernel.forward(input, 3, 256, 256, filter, 48, 256 / 2, 256 / 2, 11, 2, bias, act, true);
            fKernel.forward(input2, 48, 32, 32, filter2, 96, 16, 16, 5, 2, bias2, act, true);
            bKernel.backward(delta2, result2, input2, 48, 32, 32, filter2, 96, 16, 16, 5, 2, bias2, true);
            bKernel.backward(delta, result, input, 3, 256, 256, filter, 48, 128, 128, 11, 2, bias, true);
        });        
        
        bfKernel.backword(delta, result, input, 3, 256, 256, filter, 48, 128, 128, 11, 2, false);
        bfKernel.backword(delta, result, input, 3, 256, 256, filter, 48, 128, 128, 11, 2, true);
        bench("1st filter gpu", () ->
            bfKernel.backword(delta, result, input, 3, 256, 256, filter, 48, 128, 128, 11, 2, true));
        bench("1st filter cpu", () ->
            bfKernel.backword(delta, result, input, 3, 256, 256, filter, 48, 128, 128, 11, 2, false));
        
        bdKernel.backword(input, delta, result, 3, 256, 256, filter, 48, 128, 128, 11, 2, false);
        bdKernel.backword(input, delta, result, 3, 256, 256, filter, 48, 128, 128, 11, 2, true);
        bench("1st delta gpu", () ->
            bdKernel.backword(input, delta, result, 3, 256, 256, filter, 48, 128, 128, 11, 2, true));
        bench("1st delta cpu", () ->
            bdKernel.backword(input, delta, result, 3, 256, 256, filter, 48, 128, 128, 11, 2, false));
        
        bbKernel.backwordBias(delta, result, 48, 128, 128, bias, false);
        bbKernel.backwordBias(delta, result, 48, 128, 128, bias, true);
        bench("1st bias gpu", () ->
            bbKernel.backwordBias(delta, result, 48, 128, 128, bias, true));
        bench("1st bias cpu", () ->
            bbKernel.backwordBias(delta, result, 48, 128, 128, bias, false));
        
        bench("1st delta filter bias gpu", () ->{
            bdKernel.backword(input, delta, result, 3, 256, 256, filter, 48, 128, 128, 11, 2, true);
            bfKernel.backword(delta, result, input, 3, 256, 256, filter, 48, 128, 128, 11, 2, true);
            bbKernel.backwordBias(delta, result, 48, 128, 128, bias, true);
        });
        bench("1st delta filter bias cpu", () ->{
            bdKernel.backword(input, delta, result, 3, 256, 256, filter, 48, 128, 128, 11, 2, false);
            bfKernel.backword(delta, result, input, 3, 256, 256, filter, 48, 128, 128, 11, 2, false);
            bbKernel.backwordBias(delta, result, 48, 128, 128, bias, false);
        });
        
        fKernel.forward(input, 3, 256, 256, filter, 48, 256 / 2, 256 / 2, 11, 2, bias, act, false);
        bKernel.backward(delta, result, input, 3, 256, 256, filter, 48, 128, 128, 11, 2, bias, false);
        fKernel.forward(input, 3, 256, 256, filter, 48, 256 / 2, 256 / 2, 11, 2, bias, act, true);
        bKernel.backward(delta, result, input, 3, 256, 256, filter, 48, 128, 128, 11, 2, bias, true);
        
        
        
        bench("1st gpu", () -> 
            fKernel.forward(input, 3, 256, 256, filter, 48, 256 / 2, 256 / 2, 11, 2, bias, act, true));
        bench("1st cpu", () -> 
            fKernel.forward(input, 3, 256, 256, filter, 48, 256 / 2, 256 / 2, 11, 2, bias, act, false));
        
        bench("2nd gpu", () -> 
            fKernel.forward(input2, 48, 32, 32, filter2, 96, 16, 16, 5, 2, bias2, act, true)
        );
        bench("2nd cpu", () -> 
            fKernel.forward(input2, 48, 32, 32, filter2, 96, 16, 16, 5, 2, bias2, act, false)
        );
        
        
        bench("1st back gpu", () -> 
            bKernel.backward(delta, result, input, 3, 256, 256, filter, 48, 128, 128, 11, 2, bias, true));
        bench("1st back cpu", () -> 
            bKernel.backward(delta, result, input, 3, 256, 256, filter, 48, 128, 128, 11, 2, bias, false));

        bench("2st back gpu", () -> 
            bKernel.backward(delta2, result2, input2, 48, 32, 32, filter2, 96, 16, 16, 5, 2, bias2, true));
        bench("2st back cpu", () -> 
            bKernel.backward(delta2, result2, input2, 48, 32, 32, filter2, 96, 16, 16, 5, 2, bias2, false));
        
        bench("complex optimize gpu", () -> {
            fKernel.forward(input, 3, 256, 256, filter, 48, 256 / 2, 256 / 2, 11, 2, bias, act, true);
            fKernel.forward(input2, 48, 32, 32, filter2, 96, 16, 16, 5, 2, bias2, act, true);
            
            bdKernel.backword(input2, delta2, result2, 48, 32, 32, filter2, 96, 16, 16, 5, 2, true);
            bfKernel.backword(delta2, result2, input2, 48, 32, 32, filter2, 96, 16, 16, 5, 2, true);
            bbKernel.backwordBias(delta2, result2, 96, 16, 16, bias2, true);
            
            bdKernel.backword(input2, delta, result, 3, 256, 256, filter, 48, 128, 128, 11, 2, true);
            bfKernel.backword(delta, result, input, 3, 256, 256, filter, 48, 128, 128, 11, 2, true);
            bbKernel.backwordBias(delta, result, 48, 128, 128, bias, true);
        });
        bench("complex gpu", () -> {
            fKernel.forward(input, 3, 256, 256, filter, 48, 256 / 2, 256 / 2, 11, 2, bias, act, true);
            fKernel.forward(input2, 48, 32, 32, filter2, 96, 16, 16, 5, 2, bias2, act, true);
            bKernel.backward(delta2, result2, input2, 48, 32, 32, filter2, 96, 16, 16, 5, 2, bias2, true);
            bKernel.backward(delta, result, input, 3, 256, 256, filter, 48, 128, 128, 11, 2, bias, true);
        });
        bench("complex cpu", () -> {
            fKernel.forward(input, 3, 256, 256, filter, 48, 256 / 2, 256 / 2, 11, 2, bias, act, false);
            fKernel.forward(input2, 48, 32, 32, filter2, 96, 16, 16, 5, 2, bias2, act, false);
            bKernel.backward(delta2, result2, input2, 48, 32, 32, filter2, 96, 16, 16, 5, 2, bias2, false);
            bKernel.backward(delta, result, input, 3, 256, 256, filter, 48, 128, 128, 11, 2, bias, false);
        });
    }
    
    
    static void bench(String name, Runnable proc) {
        for (int i = 0; i < 10; ++i) {
            proc.run();
        }
        long start = System.currentTimeMillis();
        for (int i = 0; i < 100; ++i) {
            proc.run();
        }
        System.out.printf("%s:%.3fs%n", name, (System.currentTimeMillis() - start) / 1000.);
    }
}
