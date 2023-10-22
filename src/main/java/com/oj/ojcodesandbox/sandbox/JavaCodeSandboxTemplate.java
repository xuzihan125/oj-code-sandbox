package com.oj.ojcodesandbox.sandbox;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.oj.ojcodesandbox.sandbox.model.ExecuteCodeRequest;
import com.oj.ojcodesandbox.sandbox.model.ExecuteCodeRespond;
import com.oj.ojcodesandbox.sandbox.model.ExecuteMessage;
import com.oj.ojcodesandbox.sandbox.model.JudgeInfo;
import com.oj.ojcodesandbox.utils.ProcessUtis;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class JavaCodeSandboxTemplate implements CodeSandbox{

    private static final String GLOBAL_CODE_DIR_NAME = "tempoCode";

    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    private static final Long TIME_OUT = 5*1000L;

    private static final boolean firstInit = true;


    /**
     * save the code to the file
     * @param code user's code
     * @return
     */
    public File saveCodeToFile(String code){
        String userDir = System.getProperty("user.dir");
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
        // check for dir
        if (!FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }

        //seperate code from different user
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_JAVA_CLASS_NAME;
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
        return userCodeFile;
    }

    /**
     * try to compile the code file
     * @param userCodeFile file ohject that has user's code in
     * @return compile result
     */
    public ExecuteMessage compileFile(File userCodeFile) throws Exception{
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        ExecuteMessage executeCompileMessage = ProcessUtis.runProcess(compileCmd);
        System.out.println("compile complete");
        return executeCompileMessage;
    }

    /**
     * execute compiled file with given inputList
     * @param userCodeFile file contained user info
     * @param inputList list of input value
     * @return execute result
     */
    public List<ExecuteMessage> runFile(File userCodeFile, List<String> inputList) throws Exception{
        String userCodeParentPath = userCodeFile.getParent();
        List<ExecuteMessage> output = new ArrayList<>();
        for(String input : inputList){
            String runCmd = String.format("java -cp %s Main %s", userCodeParentPath, input);
            ExecuteMessage executeMessage = ProcessUtis.runProcess(runCmd);
            output.add(executeMessage);
        }
        return output;
    }

    /**
     * get output respond from the execute message
     * @param output output list after execute
     * @return respond message
     */
    public ExecuteCodeRespond getOutputRespond(List<ExecuteMessage> output){
        ExecuteCodeRespond executeCodeRespond = new ExecuteCodeRespond();
        List<String> outputResult = new ArrayList<>();
        //todo change into more detail information
        Long time = 0L;
        Long memory = 0L;
        for(ExecuteMessage executeMessage: output){
            if(StrUtil.isNotBlank(executeMessage.getErrorMessage())){
                executeCodeRespond.setMessage(executeMessage.getErrorMessage());
                executeCodeRespond.setStatus(3);
                break;
            }
            time = Math.max(time, executeMessage.getTime());
            memory = Math.max(memory, executeMessage.getMemory());
            outputResult.add(executeMessage.getMessage());
        }
        executeCodeRespond.setOutputList(outputResult);
        executeCodeRespond.setStatus(1);
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(time);
        judgeInfo.setMemory(memory);
        executeCodeRespond.setJudgeInfo(judgeInfo);

        return executeCodeRespond;
    }

    /**
     * delete the created user file
     * @param userCodeFile
     */
    public void delete(File userCodeFile){
        if (userCodeFile.getParentFile()!=null){
            boolean del = FileUtil.del(userCodeFile.getParent());
        }
    }

    /**
     * create respond when error occur
     * @param message
     * @return
     */
    private ExecuteCodeRespond getErrorRespond(String message) {
        ExecuteCodeRespond executeCodeRespond = new ExecuteCodeRespond();
        executeCodeRespond.setOutputList(new ArrayList<>());
        executeCodeRespond.setMessage(message);
        executeCodeRespond.setStatus(2);
        executeCodeRespond.setJudgeInfo(new JudgeInfo());
        return executeCodeRespond;
    }

    @Override
    public ExecuteCodeRespond executeCode(ExecuteCodeRequest executeCodeRequest) {
        String code = executeCodeRequest.getCode();
        List<String> inputList = executeCodeRequest.getInputList();
        String language = executeCodeRequest.getLanguage();

        try{
            //1. save code to file
            File userCodeFile = saveCodeToFile(code);

            //2. compile the code file
            ExecuteMessage executeCompileMessage = compileFile(userCodeFile);
            if (executeCompileMessage.getExitValue() != 0) {
                return getErrorRespond(executeCompileMessage.getErrorMessage());
            }

            //3. run file and get result
            List<ExecuteMessage> output = runFile(userCodeFile, inputList);

            //4. create respond message
            ExecuteCodeRespond outputRespond = getOutputRespond(output);

            //5. delete created file
            delete(userCodeFile);

            return outputRespond;
        }
        catch (Exception e){
            return getErrorRespond(e.getMessage());
        }
    }


}
