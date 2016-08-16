package com.jaredrummler.android.processes.models;

import java.io.IOException;


public final class Cgroup extends ProcFile
{
	public final String[] groups;
	public boolean foreground;

	
	public static Cgroup get(int pid) throws IOException 
	{
		return new Cgroup(String.format("/proc/%d/cgroup", pid));
	}

	
	private Cgroup(String path) throws IOException 
	{
		super(path);
		
		groups = content.split("\n");

		for (int i = 0; i < groups.length; i++) {
			if (groups[i].contains("cpuset")) {
				foreground = groups[i].contains("foreground");
				return;
			}
		}


		foreground = false;
	}
}
