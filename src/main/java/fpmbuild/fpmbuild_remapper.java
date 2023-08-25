package fpmbuild;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.asbestosstar.assistremapper.Mappings;
import com.asbestosstar.assistremapper.RemapperInstance;

public class fpmbuild_remapper {

	public File fpm;
	public Mappings mappings;
	public RemapperInstance remapper;
	
	public fpmbuild_remapper(String path_to_fpm, String mappings_location, String dependency_location) {
		// TODO Auto-generated constructor stub
	File fpm = new File(path_to_fpm);
	File mappings = new File(mappings_location);
	File dependency = new File(dependency_location);
	
	try {
		this.mappings=new Mappings(new FileInputStream(mappings));
		String run_dir = System.getProperty("user.dir");
		this.remapper = new RemapperInstance(this.mappings,run_dir+"/BUILD_ROOT/");
		
		//get all the files in the dependency folder
		for(File dep: dependency.listFiles())
		{
			remapper.addToClasspathJar(new JarFile(dep));
		}
		
		
	} catch (FileNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	
	
	
	}
	
	public void mapInPlace() {
try {
	remapper.remapJar(new JarFile(fpm));
	String run_dir = System.getProperty("user.dir");
File build_root= new File(run_dir+"/BUILD_ROOT/");

	
	
	 try {
         FileOutputStream fos = new FileOutputStream(fpm);
         ZipOutputStream zos = new ZipOutputStream(fos);

         
         zipFolder(build_root, build_root.getName(), zos);

         zos.close();
         fos.close();

         System.out.println("Files zipped successfully");
     } catch (IOException e) {
         e.printStackTrace();
     }
	
		for(File file: build_root.listFiles()) {
		file.deleteOnExit();	
		}
	
	
} catch (IOException e) {
	// TODO Auto-generated catch block
	e.printStackTrace();
}

	}
	
	
	
	public static void zipFolder(File folder, String superFolder, ZipOutputStream zos) throws IOException {
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
//                zipFolder(file, superFolder + "/" + file.getName(), zos);
                zipFolder(file,  file.getName(), zos);

            } else {
                byte[] buffer = new byte[1024];
                FileInputStream fis = new FileInputStream(file);
               // zos.putNextEntry(new ZipEntry(superFolder + "/" + file.getName()));
                zos.putNextEntry(new ZipEntry(file.getName()));

                int length;
                while ((length = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, length);
                }

                zos.closeEntry();
                fis.close();
            }
        }
    }
	
	
	
	
	

}
