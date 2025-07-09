/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package redescriptionmining;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

/**
 * @author matej
 */
public class ClusProcessExecutor {

    void run(String javaPath, String clusPath, String outputDirPath, String outName, int RorF, int CLUSMemory) {

        String type = "";

        if (RorF == 0)
            type = "-forest";
        else type = "-rules";

        ProcessBuilder pb = new ProcessBuilder(javaPath, "-Xmx" + CLUSMemory + "m", "-jar", clusPath, type, outName);
        pb.directory(new File(outputDirPath));
        pb.redirectErrorStream(true);
        Process p = null;
        try {
            p = pb.start();

            InputStreamReader isr = new InputStreamReader(p.getInputStream());
            BufferedReader br = new BufferedReader(isr);

            String lineRead;
            while ((lineRead = br.readLine()) != null) {
                // swallow the line, or print it out - System.out.println(lineRead);
                System.out.println(lineRead);
            }
            isr.close();
            br.close();
        } catch (java.io.IOException e) {
            e.printStackTrace();
            System.exit(-1);
        } finally {
            try {
                p.waitFor();
            } catch (java.lang.InterruptedException e1) {
                e1.printStackTrace();
            }
            try {
                p.getInputStream().close();
            } catch (java.io.IOException e) {
                e.printStackTrace();
                System.exit(-1);
            }
            try {
                p.getOutputStream().close();
            } catch (java.io.IOException e) {
                e.printStackTrace();
                System.exit(-1);
            }
            try {
                p.getErrorStream().close();
            } catch (java.io.IOException e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }
    }

}
