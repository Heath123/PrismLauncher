/* Copyright 2012-2014 MultiMC Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.multimc.onesix;

import org.multimc.*;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

public class OneSixLauncher implements Launcher
{
	@Override
	public int launch(ParamBucket params)
	{
		// get and process the launch script params
		List<String> libraries;
		List<String> mcparams;
		List<String> mods;
		String mainClass;
		String natives;
		final String windowTitle;
		String windowParams;
		try
		{
			libraries = params.all("cp");
			mcparams = params.all("param");
			mainClass = params.first("mainClass");
			mods = params.allSafe("mods", new ArrayList<String>());
			natives = params.first("natives");
			windowTitle = params.first("windowTitle");
			// windowParams = params.first("windowParams");
		} catch (NotFoundException e)
		{
			System.err.println("Not enough arguments.");
			e.printStackTrace(System.err);
			return -1;
		}

		List<String> allJars = new ArrayList<String>();
		allJars.addAll(mods);
		allJars.addAll(libraries);

		if(!Utils.addToClassPath(allJars))
		{
			System.err.println("Halting launch due to previous errors.");
			return -1;
		}

		final ClassLoader cl = ClassLoader.getSystemClassLoader();

		// print the pretty things
		{
			System.out.println("Main Class:");
			System.out.println(mainClass);
			System.out.println();

			System.out.println("Libraries:");
			for (String s : libraries)
			{
				System.out.println(s);
			}
			System.out.println();

			if(mods.size() > 0)
			{
				System.out.println("Class Path Mods:");
				for (String s : mods)
				{
					System.out.println(s);
				}
				System.out.println();
			}

			System.out.println("Params:");
			System.out.println(mcparams.toString());
			System.out.println();
		}

		// set up the natives path(s).
		System.setProperty("java.library.path", natives );
		Field fieldSysPath;
		try
		{
			fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
			fieldSysPath.setAccessible( true );
			fieldSysPath.set( null, null );
		} catch (Exception e)
		{
			System.err.println("Failed to set the native library path:");
			e.printStackTrace(System.err);
			return -1;
		}

		// Get the Minecraft Class.
		Class<?> mc;
		try
		{
			mc = cl.loadClass(mainClass);
		} catch (ClassNotFoundException e)
		{
			System.err.println("Failed to find Minecraft main class:");
			e.printStackTrace(System.err);
			return -1;
		}

		// get the main method.
		Method meth;
		try
		{
			meth = mc.getMethod("main", String[].class);
		} catch (NoSuchMethodException e)
		{
			System.err.println("Failed to acquire the main method:");
			e.printStackTrace(System.err);
			return -1;
		}

		// FIXME: works only on linux, we need a better solution
/*
		final java.nio.ByteBuffer[] icons = IconLoader.load("icon.png");
		new Thread() {
			public void run() {
				ClassLoader cl = ClassLoader.getSystemClassLoader();
				try
				{
					Class<?> Display;
					Method isCreated;
					Method setTitle;
					Method setIcon;

					Display = cl.loadClass("org.lwjgl.opengl.Display");
					isCreated = Display.getMethod("isCreated");
					setTitle = Display.getMethod("setTitle", String.class);
					setIcon = Display.getMethod("setIcon", java.nio.ByteBuffer[].class);

					// set the window title? Maybe?
					while(!(Boolean) isCreated.invoke(null))
					{
						try
						{
							Thread.sleep(150);
						} catch (InterruptedException ignored) {}
					}
					// Give it a bit more time ;)
					Thread.sleep(150);
					// set the title
					setTitle.invoke(null,windowTitle);
					// only set icon when there's actually something to set...
					if(icons.length > 0)
					{
						setIcon.invoke(null,(Object)icons);
					}
				}
				catch (Exception e)
				{
					System.err.println("Couldn't set window icon or title.");
					e.printStackTrace(System.err);
				}
			}
		}
		.start();
*/
		// start Minecraft
		String[] paramsArray = mcparams.toArray(new String[mcparams.size()]); // init params accordingly
		try
		{
			meth.invoke(null, (Object) paramsArray); // static method doesn't have an instance
		} catch (Exception e)
		{
			System.err.println("Failed to start Minecraft:");
			e.printStackTrace(System.err);
			return -1;
		}
		return 0;
	}
}
