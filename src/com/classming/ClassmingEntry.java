package com.classming;

import com.classming.Vector.LevenshteinDistance;
import com.classming.Vector.MathTool;
import com.classming.record.Recover;
import soot.jimple.Stmt;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ClassmingEntry {

    public static MutateClass randomMutation(MutateClass target) throws IOException {
        Random random = new Random();
        int randomAction = random.nextInt(3);
        switch (randomAction) {
            case 0:
                return target.iteration();
            case 1:
                return target.lookUpSwitchIteration();
            case 2:
                return target.returnIteration();
        }
        return null;
    }

    public static void process(String className, int iterationCount, String[] args, String classPath, String dependencies) throws IOException {
        if(classPath!=null && !classPath.equals("")){
            Main.setGenerated(classPath);
        }
        if(dependencies!=null && !dependencies.equals("")){
            Main.setDependencies(dependencies);
        }
        MutateClass mutateClass = new MutateClass();
        Main.initial(args);
        mutateClass.initialize(className, args);
        List<MutateClass> mutateAcceptHistory = new ArrayList<>();
        List<MutateClass> mutateRejectHistory = new ArrayList<>();
        List<Double> averageDistance = new ArrayList<>();
        Random random = new Random();
        mutateAcceptHistory.add(mutateClass);
        mutateClass.saveCurrentClass();
        for (int i = 0; i < iterationCount; i ++) {
            System.out.println("Current size is : " + mutateAcceptHistory.size() + ", iteration is :" + i);
            MutateClass newOne = randomMutation(mutateClass); // sootclass has changed here for all objects.
            if (newOne != null) {
                MutateClass previousClass = mutateAcceptHistory.get(mutateAcceptHistory.size() - 1);
                MethodCounter current = newOne.getCurrentMethod();
                List<String> currentLiveCode = newOne.getMethodLiveCodeString(current.getSignature());
                List<String> originalCode = previousClass.getMethodOriginalStmtListString(current.getSignature());
                int distance = LevenshteinDistance.computeLevenshteinDistance(currentLiveCode, previousClass.getMethodLiveCodeString(current.getSignature()));
                double covScore = calculateCovScore(newOne);
                double rand = random.nextDouble();
                double fitnessScore = fitness(calculateCovScore(mutateClass), covScore, originalCode.size());
                if(rand < fitnessScore) {
                    System.out.println(covScore);
                    System.out.println("Distance is " + distance + " signature is " + current.getSignature());
                    averageDistance.add(distance / 1.0);
                    showListElement(currentLiveCode);
                    showListElement(previousClass.getMethodLiveCodeString(current.getSignature()));
                    mutateAcceptHistory.add(newOne);
                    mutateClass = newOne;
                } else {
                    mutateRejectHistory.add(newOne);
                    mutateClass = Recover.recoverFromPath(mutateAcceptHistory.get(mutateAcceptHistory.size() - 1));
                }

            } else {
                mutateClass = Recover.recoverFromPath(mutateAcceptHistory.get(mutateAcceptHistory.size() - 1));
//                System.out.println(mutateClass.getBackPath());
            }
        }
        System.out.println("Average distance is " + MathTool.mean(averageDistance));
        System.out.println("var is " + MathTool.standardDeviation(averageDistance));
        Recover.recoverFromPath(mutateAcceptHistory.get(0));
    }

    public static double fitness(double previousCov, double currentCov, int total) {
        double result = Math.exp(0.08 * total * (previousCov - currentCov));
        return 1.0 < result ? 1.0 : result;
    }

    public static double calculateCovScore(MutateClass mutateClass) {
        MethodCounter current = mutateClass.getCurrentMethod();
        List<String> currentLiveCode = mutateClass.getMethodLiveCodeString(current.getSignature());
        List<String> originalCode = mutateClass.getMethodOriginalStmtListString(current.getSignature());
        return currentLiveCode.size() / (double)originalCode.size();
    }

    public static void showListElement(List<String> target) {
        StringBuilder builder = new StringBuilder();
        for (String element: target) {
            builder.append(element + " ");
        }
        System.out.println(builder);
    }


    public static void main(String[] args) throws IOException {
        process("com.classming.Hello", 500, args, null, "");
//        process("avrora.Main", 500, new String[]{"-action=cfg","sootOutput/avrora-cvs-20091224/example.asm"}, "./sootOutput/avrora-cvs-20091224/",null);
//        process("net.sourceforge.pmd.PMD", 500, new String[]{"./src","text", "unusedcode"}, "./sootOutput/pmd-4.2.5/", "dependencies/jaxen-1.1.1.jar;dependencies/asm-3.1.jar");
//        process("org.sunflow.Benchmark", 500, args, "./sootOutput/sunflow-0.07.2/", "dependencies/janino-2.5.15.jar");
//        process("org.eclipse.core.runtime.adaptor.EclipseStarter", 100, args, "./sootOutput/eclipse/", null); // no arguments?
    }



}
