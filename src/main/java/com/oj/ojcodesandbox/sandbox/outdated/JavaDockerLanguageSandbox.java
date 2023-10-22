package com.oj.ojcodesandbox.sandbox.outdated;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.oj.ojcodesandbox.sandbox.CodeSandbox;
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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class JavaDockerLanguageSandbox implements CodeSandbox {

    private static final String GLOBAL_CODE_DIR_NAME = "tempoCode";

    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    private static final Long TIME_OUT = 5*1000L;

    private static final boolean firstInit = true;

    public static void main(String args[]) {
        JavaDockerLanguageSandbox tempo = new JavaDockerLanguageSandbox();
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2", "2 3"));
        String code = ResourceUtil.readStr("testcode/simpleComputeArgs/SimpleCompute.java", StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");
        System.out.println("program start");
        ExecuteCodeRespond executeCodeRespond = tempo.executeCode(executeCodeRequest);
        System.out.println(executeCodeRespond);
    }

    //todo javasecurity manager
    @Override
    public ExecuteCodeRespond executeCode(ExecuteCodeRequest executeCodeRequest) {
        String code = executeCodeRequest.getCode();
        List<String> inputList = executeCodeRequest.getInputList();
        String language = executeCodeRequest.getLanguage();

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
        System.out.println("write file complete");

        //compile
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        ExecuteMessage executeCompileMessage = ProcessUtis.runProcess(compileCmd);
        if (executeCompileMessage.getExitValue() != 0) {
            return getErrorRespond(executeCompileMessage.getErrorMessage());
        }
        System.out.println("compile complete");
//        List<ExecuteMessage> output = new ArrayList<>();
//        for (String input : inputList) {
//            String runCmd = String.format("java -cp %s Main %s", userCodeParentPath, input);
//            ExecuteMessage executeMessage = ProcessUtis.runProcess(runCmd);
//            output.add(executeMessage);
//

        // create docker container
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();
        String image = "openjdk:8-alpine";
        // pull image
        if (firstInit) {
            PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
            PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
                @Override
                public void onNext(PullResponseItem item) {
                    System.out.println("download image:" + item.getStatus());
                    super.onNext(item);
                }
            };
            try {
                pullImageCmd.exec(pullImageResultCallback).awaitCompletion();
            } catch (InterruptedException e){
                System.out.println("exception when trying to pull image");
                throw new RuntimeException(e);
            }
//            firstInit = false;
        }
//        System.out.println("execute complete");

        // create container with compiled file inside
        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image);
        HostConfig hostConfig = new HostConfig();
        hostConfig
                .withMemory(100*1024*1024L)
                .withMemorySwap(0L)
                .withCpuCount(1L)
                .setBinds(new Bind(userCodeParentPath, new Volume("/app/code")));
        CreateContainerResponse exec = containerCmd
                .withHostConfig(hostConfig)
                .withNetworkDisabled(true)
                .withReadonlyRootfs(true)
                .withAttachStdin(true)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withTty(true)
                .exec();
        System.out.println(exec);
        String id = exec.getId();

        //start container
        dockerClient.startContainerCmd(id).exec();
        List<ExecuteMessage> output = new ArrayList<>();
        for(String inputArgs : inputList){
            StopWatch stopWatch = new StopWatch();
            Long time = 0L;
            String[] args = inputArgs.split(" ");
            String[] cmdArray = ArrayUtil.append(new String[]{"java", "-cp", "/app/code", "Main"}, args);
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(id)
                    .withCmd(cmdArray)
                    .withAttachStdin(true)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
//                .withTty(true)
                    .exec();
            System.out.println("create execute command:" + execCreateCmdResponse);

            // execute command and get result
            ExecuteMessage executeMessage = new ExecuteMessage();
            final String[] message = {null};
            final String[] errorMessage = {null};
            final boolean[] timeOut = {true};
            String execCreateCmdResponseId = execCreateCmdResponse.getId();
            ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback() {
                @Override
                public void onComplete() {
                    timeOut[0] = false;
                    super.onComplete();
                }

                @Override
                public void onNext(Frame frame) {
                    StreamType streamType = frame.getStreamType();
                    if(StreamType.STDERR.equals(streamType)){
                        errorMessage[0] = new String(frame.getPayload());
                        System.out.println("execute error result:" + errorMessage[0]);
                    }
                    else{
                        message[0] = new String(frame.getPayload());
                        System.out.println("execute result:" + message[0]);
                    }

                    super.onNext(frame);
                }
            };
            //get memory usage
            final Long[] maxMemory = {0L};
            StatsCmd statsCmd = dockerClient.statsCmd(id);
            ResultCallback<Statistics> resultCallback = new ResultCallback<Statistics>() {
                @Override
                public void onNext(Statistics statistics) {
                    maxMemory[0] = Math.max(maxMemory[0], statistics.getMemoryStats().getUsage());
                    System.out.println("memory usage:" + maxMemory[0]);
                }

                @Override
                public void onStart(Closeable closeable) {

                }

                @Override
                public void onError(Throwable throwable) {

                }

                @Override
                public void onComplete() {

                }

                @Override
                public void close() throws IOException {

                }
            };
            statsCmd.exec(resultCallback);

            try{
                stopWatch.start();
                dockerClient
                        .execStartCmd(execCreateCmdResponseId)
                        .exec(execStartResultCallback)
                        .awaitCompletion(TIME_OUT, TimeUnit.MICROSECONDS);
                stopWatch.stop();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if(timeOut[0]){
                return getErrorRespond("answer exceed time limit");
            }
            statsCmd.close();
            executeMessage.setMessage(message[0]);
            executeMessage.setErrorMessage(errorMessage[0]);
            executeMessage.setTime(time);
            executeMessage.setMemory(maxMemory[0]);
            output.add(executeMessage);

        }



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

        //delete created file
        if (userCodeFile.getParentFile()!=null){
            boolean del = FileUtil.del(userCodeParentPath);
        }
        return executeCodeRespond;
    }

    private ExecuteCodeRespond getErrorRespond(String message) {
        ExecuteCodeRespond executeCodeRespond = new ExecuteCodeRespond();
        executeCodeRespond.setOutputList(new ArrayList<>());
        executeCodeRespond.setMessage(message);
        executeCodeRespond.setStatus(2);
        executeCodeRespond.setJudgeInfo(new JudgeInfo());
        return executeCodeRespond;
    }

}
