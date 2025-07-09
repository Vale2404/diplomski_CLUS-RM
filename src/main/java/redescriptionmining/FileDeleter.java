/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package redescriptionmining;

import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *
 * @author matej
 */
public class FileDeleter {
    String path;

    public void setPath(String pt){
        path=pt;
    }

    public void delete(){
        Path ph=Paths.get(path);

      try {
            Files.deleteIfExists(ph);
            }
      catch (NoSuchFileException x) {
                System.err.format("%s: no such" + " file or directory%n", ph);
            } catch (DirectoryNotEmptyException x) {
                System.err.format("%s not empty%n", ph);
            } catch (IOException x) {
    // File permission problems are caught here.
                System.err.println(x);
            }
    }
}
