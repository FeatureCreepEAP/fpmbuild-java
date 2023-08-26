
/**
 * 
 */
package fpmbuild;

/**
 * @author rhel
 *
 */
public class fpmbuild {

	/**
	 * 
	 */
	public fpmbuild() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println(System.getProperty("user.dir"));
if(args.length== 0) {
	System.out.println("Usage: java fpmbuild args");
 //   System.exit(1);
}else {
	if(args[0].equals("-ba")) {
		//Install
	}else if (args[0].equals("-bb")) {
		
	}else if (args[0].equals("-bs")) {
		
	}else if (args[0].equals("-remap")) {
		fpmbuild_remapper remapper = new fpmbuild_remapper(args[1], args[2], args[3]);
		remapper.mapInPlace();
	}
	
	
	
}
		
		
		
	}

}
