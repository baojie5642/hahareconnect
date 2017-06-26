package com.baojie.liuxinreconnect.util.HaFileWatcher;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;


/**
 * Created by baojie on 17-6-26.
 */
public class DirectoryWatcherFixture {

    private static final String DIR_PATH =System.getProperty("user.dir");
    private static final File DIR = new File(DIR_PATH);
    private static final String SUFFIX = ".txt";
    private static final String PREFIX = "test";
    private static final int ADD_TIMES = 3;

    /**
     * 观察者
     * @author wangxiang
     *
     */
    public class Logger implements Observer {
        @Override
        public void update(Observable observable, Object eventArgs) {
            FileSystemEventArgs args = (FileSystemEventArgs) eventArgs;
            System.out.printf("%s has been %s\n", args.getFileName(), args.getKind());
           // assertTrue(args.getFileName().startsWith(PREFIX));
           // assertEquals(ENTRY_CREATE, args.getKind());
        }
    }

    //@Test
    public void testWatchFile() throws IOException, InterruptedException{
        DirectoryWatcher watcher = new DirectoryWatcher(DIR_PATH);
        Logger l1 = new Logger();
        watcher.addObserver(l1);
        watcher.execute();

        //    创建一系列临时文件
        List<String> files = new ArrayList<>();
        for(int i=0; i<ADD_TIMES; i++){
            files.add(File.createTempFile(PREFIX, SUFFIX, DIR).toString());
        }

        //    延迟等待后台任务的执行
        Thread.sleep(4000);
        watcher.shutdown();
        System.out.println("finished");
    }
}
