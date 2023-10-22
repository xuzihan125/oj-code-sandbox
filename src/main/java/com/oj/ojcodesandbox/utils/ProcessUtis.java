package com.oj.ojcodesandbox.utils;

import com.oj.ojcodesandbox.sandbox.model.ExecuteMessage;
import org.springframework.util.StopWatch;

import java.io.*;

public class ProcessUtis {

    public static String getOutPut(InputStream inputStream){
        try{
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder builder = new StringBuilder();
            String compileOutputLine;
            while((compileOutputLine = bufferedReader.readLine()) != null){
                builder.append(compileOutputLine);
            }
            return builder.toString();
        }
        catch (IOException e){
            return e.getMessage();
        }
    }

    public static ExecuteMessage runProcess(String commend){

        ExecuteMessage executeMessage = new ExecuteMessage();
        try{
            StopWatch stopWatch = new StopWatch();
            Process process = Runtime.getRuntime().exec(commend);
            stopWatch.start();
            int exitValue = process.waitFor();
            stopWatch.stop();
            executeMessage.setTime(stopWatch.getLastTaskTimeMillis());
            executeMessage.setMemory(0L);
            executeMessage.setExitValue(exitValue);
            if(exitValue == 0){
                String compileSuccessMessage = ProcessUtis.getOutPut(process.getInputStream());
                executeMessage.setMessage(compileSuccessMessage);
            }
            else{
                String compileFailMessage = ProcessUtis.getOutPut(process.getErrorStream());
                executeMessage.setErrorMessage(compileFailMessage);
            }
        } catch (InterruptedException | IOException e){
            executeMessage.setExitValue(-1);
            executeMessage.setErrorMessage(e.getMessage());
        }
        return executeMessage;
    }

    public static ExecuteMessage runProcessThroughInput(String commend, String opName){

        ExecuteMessage executeMessage = new ExecuteMessage();
        try{
            Process process = Runtime.getRuntime().exec(commend);
            InputStream inputStream = process.getInputStream();
            OutputStream outputStream = process.getOutputStream();
            // input value into the program
            // each args end with '\n'
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
            outputStreamWriter.write(opName);
            outputStreamWriter.flush();

            int exitValue = process.waitFor();
            executeMessage.setExitValue(exitValue);
            if(exitValue == 0){
                String compileSuccessMessage = ProcessUtis.getOutPut(inputStream);
                executeMessage.setMessage(compileSuccessMessage);
                System.out.println("success"+compileSuccessMessage);
            }
            else{
                String compileFailMessage = ProcessUtis.getOutPut(process.getErrorStream());
                executeMessage.setErrorMessage(compileFailMessage);
                System.out.println("fail " + compileFailMessage);
            }
            outputStreamWriter.close();
            outputStream.close();
            inputStream.close();
            process.destroy();
        } catch (InterruptedException | IOException e){
            executeMessage.setExitValue(-1);
            executeMessage.setErrorMessage(e.getMessage());
        }
        return executeMessage;
    }
}
