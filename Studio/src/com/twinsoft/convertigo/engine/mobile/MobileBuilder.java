/*
 * Copyright (c) 2001-2016 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.engine.mobile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FalseFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;

import com.twinsoft.convertigo.beans.core.MobileApplication;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.mobile.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.mobile.components.PageComponent;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.FileUtils;

public class MobileBuilder {
	
	private Project project = null;
	boolean initDone = false;
	
	File projectDir, ionicTplDir, ionicWorkDir;
	
	static public void initBuilder(Project project) {
		if (project != null) {
			try {
				project.getMobileBuilder().init();
			} catch (Exception e) {
				Engine.logEngine.error("Failed to initialize mobile builder for project \""+project.getName()+"\"", e);
			}
		}
	}
	
	static public void releaseBuilder(Project project) {
		if (project != null) {
			try {
				project.getMobileBuilder().release();
			} catch (Exception e) {
				Engine.logEngine.error("Failed to release mobile builder for project \""+project.getName()+"\"", e);
			}
		}
	}
	
	public MobileBuilder(Project project) {
		this.project = project;
		
		projectDir = new File(project.getDirPath());
		ionicTplDir = new File(projectDir,"ionicTpl");
		ionicWorkDir = new File(projectDir,"_private/ionic");
	}
	
	public boolean hasNodeModules() {
		File nodeModulesDir = new File(ionicWorkDir,"node_modules");
		return nodeModulesDir.exists();
	}
		
	public synchronized void appRootChanged(final ApplicationComponent app) throws EngineException {
		if (app != null && initDone) {
			writeAppComponentTs(app);
			File appComponentTsFile = new File(ionicWorkDir, "src/app/app.component.temp.ts");
			if (appComponentTsFile.exists()) {
				writeAppComponentTempTs(app);
			}
			Engine.logEngine.debug("(MobileBuilder) Handled 'appRootChanged'");
		}
	}

	public synchronized void appRouteChanged(final ApplicationComponent app) throws EngineException {
		if (app != null && initDone) {
			writeAppComponentTs(app);
			File appComponentTsFile = new File(ionicWorkDir, "src/app/app.component.temp.ts");
			if (appComponentTsFile.exists()) {
				writeAppComponentTempTs(app);
			}
			Engine.logEngine.debug("(MobileBuilder) Handled 'appRouteChanged'");
		}
	}
	
	private void addPage(PageComponent page) throws EngineException {
		MobileApplication mobileApplication = project.getMobileApplication();
		if (mobileApplication != null) {
			ApplicationComponent application = mobileApplication.getApplicationComponent();
			if (application != null) {
				writePageSourceFiles(page);
				writeAppSourceFiles(application);
			}
		}
	}
	
	private void removePage(PageComponent page) throws EngineException {
		MobileApplication mobileApplication = project.getMobileApplication();
		if (mobileApplication != null) {
			ApplicationComponent application = mobileApplication.getApplicationComponent();
			if (application != null) {
				writeAppSourceFiles(application);
				removeUselessPage(page.getName());
			}
		}
	}
	
	public synchronized void pageEnabled(final PageComponent page) throws EngineException {
		if (page != null && page.isEnabled() && initDone) {
			addPage(page);
			Engine.logEngine.debug("(MobileBuilder) Handled 'pageEnabled'");
		}
	}
	
	public synchronized void pageDisabled(final PageComponent page) throws EngineException {
		if (page != null && !page.isEnabled() && initDone) {
			removePage(page);
			Engine.logEngine.debug("(MobileBuilder) Handled 'pageDisabled'");
		}
	}

	public synchronized void pageAdded(final PageComponent page) throws EngineException {
		if (page != null && page.isEnabled() && page.bNew && initDone) {
			addPage(page);
			Engine.logEngine.debug("(MobileBuilder) Handled 'pageAdded'");
		}
	}
	
	public synchronized void pageRemoved(final PageComponent page) throws EngineException {
		if (page != null && page.isEnabled() && initDone) {
			removePage(page);
			Engine.logEngine.debug("(MobileBuilder) Handled 'pageRemoved'");
		}
	}
	
	public synchronized void pageRenamed(final PageComponent page, final String oldName) throws EngineException {
		if (page != null && page.isEnabled() && initDone) {
			MobileApplication mobileApplication = project.getMobileApplication();
			if (mobileApplication != null) {
				ApplicationComponent application = mobileApplication.getApplicationComponent();
				if (application != null) {
					writePageSourceFiles(page);
					writeAppSourceFiles(application);
					removeUselessPage(oldName);
					Engine.logEngine.debug("(MobileBuilder) Handled 'pageRenamed'");
				}
			}
		}
	}
	
	public synchronized void pageTemplateChanged(final PageComponent page) throws EngineException {
		if (page != null && page.isEnabled() && initDone) {
			writePageTemplate(page);
			Engine.logEngine.debug("(MobileBuilder) Handled 'pageComputed'");
		}
	}
	
	public synchronized void pageStyleChanged(final PageComponent page) throws EngineException {
		if (page != null && page.isEnabled() && initDone) {
			writePageStyle(page);
			Engine.logEngine.debug("(MobileBuilder) Handled 'pageStyleChanged'");
		}
	}
	
	public synchronized void pageTsChanged(final PageComponent page) throws EngineException {
		if (page != null && page.isEnabled() && initDone) {
			writePageTs(page);
			Engine.logEngine.debug("(MobileBuilder) Handled 'pageTsChanged'");
		}
	}

	public synchronized void appStyleChanged(final ApplicationComponent app) throws EngineException {
		if (app != null && initDone) {
			writeAppStyle(app);
			Engine.logEngine.debug("(MobileBuilder) Handled 'appStyleChanged'");
		}
	}

	public synchronized void appTemplateChanged(final ApplicationComponent app) throws EngineException {
		if (app != null && initDone) {
			writeAppTemplate(app);
			Engine.logEngine.debug("(MobileBuilder) Handled 'appTemplateChanged'");
		}
	}
	
	public synchronized void appThemeChanged(final ApplicationComponent app) throws EngineException {
		if (app != null && initDone) {
			writeAppTheme(app);
			Engine.logEngine.debug("(MobileBuilder) Handled 'appThemeChanged'");
		}
	}
	
	public synchronized void appCompTsChanged(final ApplicationComponent app) throws EngineException {
		if (app != null && initDone) {
			writeAppComponentTs(app);
			Engine.logEngine.debug("(MobileBuilder) Handled 'appCompTsChanged'");
		}
	}
	
	public synchronized void appModuleTsChanged(final ApplicationComponent app) throws EngineException {
		if (app != null && initDone) {
			writeAppModuleTs(app);
			Engine.logEngine.debug("(MobileBuilder) Handled 'appModuleTsChanged'");
		}
	}
	
	private boolean isIonicTemplateBased() {
		return ionicTplDir.exists();
	}
	
	private synchronized void init() throws EngineException {
		if (isIonicTemplateBased()) {
			// Copy template directory to working directory
			copyTemplateFiles();
			
			// Modify configuration files
			updateConfigurationFiles();
			
			// Write source files (based on bean components)
			updateSourceFiles();
		}
		initDone = true;
		Engine.logEngine.debug("(MobileBuilder) Initialized builder for ionic project '"+ project.getName() +"')");
	}
	
	private synchronized void release() throws EngineException {
		if (isIonicTemplateBased()) {
			// TODO ?
		}
		
		initDone = false;
		Engine.logEngine.debug("(MobileBuilder) Released builder for ionic project '"+ project.getName() +"')");
	}
		
	private void copyTemplateFiles() throws EngineException {
		try {
			FileUtils.copyDirectory(ionicTplDir, ionicWorkDir);
			Engine.logEngine.debug("(MobileBuilder) Ionic template files copied");
		}
		catch (Exception e) {
			throw new EngineException("Unable to copy ionic template files",e);
		}
	}
	
	private void updateConfigurationFiles() throws EngineException {
		try {
			IOFileFilter fileFilter = FileFilterUtils.or(FileFilterUtils.suffixFileFilter("json"),FileFilterUtils.suffixFileFilter("xml"),FileFilterUtils.suffixFileFilter("js"));
			IOFileFilter dirFilter = FileFilterUtils.or(FileFilterUtils.nameFileFilter("config"));
			for (File f: FileUtils.listFiles(ionicWorkDir, fileFilter, dirFilter)) {
				String content = FileUtils.readFileToString(f, "UTF-8");
				content = content.replaceAll("../DisplayObjects","../../DisplayObjects");
				content = content.replaceAll("../Flashupdate","../../Flashupdate");
				FileUtils.write(f, content, "UTF-8");
			}
			Engine.logEngine.debug("(MobileBuilder) Ionic configuration files updated");
		}
		catch (Exception e) {
			throw new EngineException("Unable to update ionic configuration files",e);
		}
	}
	
	private List<PageComponent> getEnabledPages(final ApplicationComponent application) {
		List<PageComponent> pages = new ArrayList<PageComponent>();
		for (PageComponent page : application.getPageComponentList()) {
			if (page.isEnabled()) {
				pages.add(page);
			}
		}
		return pages;
	}
	
	private void updateSourceFiles() throws EngineException {
		try {
			MobileApplication mobileApplication = project.getMobileApplication();
			if (mobileApplication != null) {
				ApplicationComponent application = mobileApplication.getApplicationComponent();
				if (application != null) {
					for (PageComponent page : getEnabledPages(application)) {
						writePageSourceFiles(page);
					}
					writeAppSourceFiles(application);
					removeUselessPages(application);
					
					Engine.logEngine.debug("(MobileBuilder) Ionic source files updated");
				}
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to update ionic source files",e);
		}
	}
		
	private void writePageTemplate(PageComponent page) throws EngineException {
		try {
			if (page != null && page.isEnabled()) {
				String pageName = page.getName();
				File pageDir = new File(ionicWorkDir, "src/pages/"+pageName);
				File pageHtmlFile = new File(pageDir, pageName.toLowerCase() + ".html");
				String computedTemplate = page.getComputedTemplate();
				FileUtils.write(pageHtmlFile, computedTemplate, "UTF-8");
				
				Engine.logEngine.debug("(MobileBuilder) Ionic template file generated for page '"+pageName+"'");
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write ionic page template file",e);
		}
	}

	private void writePageStyle(PageComponent page) throws EngineException {
		try {
			if (page != null && page.isEnabled()) {
				String pageName = page.getName();
				File pageDir = new File(ionicWorkDir, "src/pages/"+pageName);
				File pageScssFile = new File(pageDir, pageName.toLowerCase() + ".scss");
				String computedScss = page.getComputedStyle();
				FileUtils.write(pageScssFile, computedScss, "UTF-8");
				
				Engine.logEngine.debug("(MobileBuilder) Ionic scss file generated for page '"+pageName+"'");
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write ionic page scss file",e);
		}
	}
	
	public String getTempTsRelativePath(PageComponent page) throws EngineException {
		try {
			if (page != null) {
				String pageName = page.getName();
				File pageDir = new File(ionicWorkDir, "src/pages/"+pageName);
				File tempTsFile = new File(pageDir, pageName.toLowerCase() + ".temp.ts");
				if (!tempTsFile.exists()) {
					writePageTempTs(page);
				}
				String filePath = tempTsFile.getPath().replace(projectDir.getPath(), "/");
				return filePath;
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write page temp ts file",e);
		}
		return null;
	}
	
	public String getTempTsRelativePath(PageComponent page, String functionMarker) throws EngineException {
		try {
			if (page != null) {
				String pageName = page.getName();
				File pageDir = new File(ionicWorkDir, "src/pages/"+pageName);
				
				String tsContent;
				if (page.isEnabled()) {
					File pageTsFile = new File(pageDir, pageName.toLowerCase() + ".ts");
					tsContent = FileUtils.readFileToString(pageTsFile, "UTF-8");
				} else {
					tsContent = getPageTsContent(page);
				}
				
				// Replace all Begin_c8o_XXX, End_c8o_XXX except for functionMarker
				Pattern pattern = Pattern.compile("/\\*Begin_c8o_(.+)\\*/");
				Matcher matcher = pattern.matcher(tsContent);
				while (matcher.find()) {
					String markerId = matcher.group(1);
					if (!markerId.equals(functionMarker)) {
						String beginMarker = "/*Begin_c8o_" + markerId + "*/";
						String endMarker = "/*End_c8o_" + markerId + "*/";
						tsContent = tsContent.replace(beginMarker, "//---"+markerId+"---");
						tsContent = tsContent.replace(endMarker, "//---"+markerId+"---");
					}
				}
				
				File tempTsFile = new File(pageDir, pageName.toLowerCase() + ".function.temp.ts");
				FileUtils.write(tempTsFile, tsContent, "UTF-8");
				String filePath = tempTsFile.getPath().replace(projectDir.getPath(), "/");
				return filePath;
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write function temp ts file",e);
		}
		return null;
	}

	private void writePageTempTs(PageComponent page) throws EngineException {
		try {
			if (page != null) {
				String pageName = page.getName();
				File pageDir = new File(ionicWorkDir, "src/pages/"+pageName);
				
				String tsContent;
				if (page.isEnabled()) {
					File pageTsFile = new File(pageDir, pageName.toLowerCase() + ".ts");
					tsContent = FileUtils.readFileToString(pageTsFile, "UTF-8");
				} else {
					tsContent = getPageTsContent(page);
				}
				
				// Replace all Begin_c8o_function:XXX, End_c8o_function:XXX
				Pattern pattern = Pattern.compile("/\\*Begin_c8o_function:(.+)\\*/");
				Matcher matcher = pattern.matcher(tsContent);
				while (matcher.find()) {
					String markerId = matcher.group(1);
					String beginMarker = "/*Begin_c8o_function:" + markerId + "*/";
					String endMarker = "/*End_c8o_function:" + markerId + "*/";
					tsContent = tsContent.replace(beginMarker, "//---"+markerId+"---");
					tsContent = tsContent.replace(endMarker, "//---"+markerId+"---");
				}
				
				// Remove all CTSXXX
				int index = tsContent.indexOf("/*End_c8o_PageFunction*/");
				if (index != -1) {
					tsContent = tsContent.substring(0, index) + "/*End_c8o_PageFunction*/"
									+ System.lineSeparator() + "}";
				}
				
				File tempTsFile = new File(pageDir, pageName.toLowerCase() + ".temp.ts");
				FileUtils.write(tempTsFile, tsContent, "UTF-8");
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write ionic page temp ts file",e);
		}
	}
	
	private void writePageTs(PageComponent page) throws EngineException {
		try {
			if (page != null && page.isEnabled()) {
				String pageName = page.getName();
				File pageDir = new File(ionicWorkDir, "src/pages/"+pageName);
				File pageTsFile = new File(pageDir, pageName.toLowerCase() + ".ts");
				FileUtils.write(pageTsFile, getPageTsContent(page), "UTF-8");
				
				Engine.logEngine.debug("(MobileBuilder) Ionic ts file generated for page '"+pageName+"'");
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write ionic page ts file",e);
		}
	}
	
	
	private String getPageTsContent(PageComponent page) throws IOException {
		String pageName = page.getName();
		String c8o_PageName = pageName;
		String c8o_PageTplUrl = pageName.toLowerCase() + ".html";
		String c8o_PageSelector = "page-"+pageName.toLowerCase();
		String c8o_PageImports = page.getComputedImports();
		String c8o_PageDeclarations = page.getComputedDeclarations();
		String c8o_PageConstructors = page.getComputedConstructors();
		String c8o_PageFunctions = page.getComputedFunctions();
		String c8o_UserCustoms = page.getScriptContent().getString();
		
		File pageTplTs = new File(ionicTplDir, "src/page.tpl");
		String tsContent = FileUtils.readFileToString(pageTplTs, "UTF-8");
		tsContent = tsContent.replaceAll("/\\*\\=c8o_PageSelector\\*/","'"+c8o_PageSelector+"'");
		tsContent = tsContent.replaceAll("/\\*\\=c8o_PageTplUrl\\*/","'"+c8o_PageTplUrl+"'");
		tsContent = tsContent.replaceAll("/\\*\\=c8o_PageName\\*/",c8o_PageName);
		tsContent = tsContent.replaceAll("/\\*\\=c8o_PageImports\\*/",c8o_PageImports);
		tsContent = tsContent.replaceAll("/\\*\\=c8o_PageDeclarations\\*/",c8o_PageDeclarations);
		tsContent = tsContent.replaceAll("/\\*\\=c8o_PageConstructors\\*/",c8o_PageConstructors);
		
		Pattern pattern = Pattern.compile("/\\*Begin_c8o_(.+)\\*/"); // begin c8o marker
		Matcher matcher = pattern.matcher(tsContent);
		while (matcher.find()) {
			String markerId = matcher.group(1);
			String tplMarker = getMarker(tsContent, markerId);
			String customMarker = getMarker(c8o_UserCustoms, markerId);
			if (!customMarker.isEmpty()) {
				tsContent = tsContent.replace(tplMarker, customMarker);
			}
		}
		
		tsContent = tsContent.replaceAll("/\\*\\=c8o_PageFunctions\\*/",c8o_PageFunctions);
		
		return tsContent;
	}
	
	public String getTempTsRelativePath(ApplicationComponent app) throws EngineException {
		try {
			if (app != null) {
				File appComponentTsFile = new File(ionicWorkDir, "src/app/app.component.temp.ts");
				if (!appComponentTsFile.exists()) {
					writeAppComponentTempTs(app);
				}
				String filePath = appComponentTsFile.getPath().replace(projectDir.getPath(), "/");
				return filePath;
			}
		}
		catch (Exception e) {}
		return null;
	}
	
	private void writeAppModuleTs(ApplicationComponent app) throws EngineException {
		try {
			if (app != null) {
				String c8o_PagesImport = "";
				String c8o_PagesLinks = "";
				String c8o_PagesDeclarations = "";
				int i=1;
				
				
				List<PageComponent> pages = getEnabledPages(app);
				for (PageComponent page : pages) {
					String pageName = page.getName();
					String pageSegment = page.getSegment();
					boolean isLastPage = i == pages.size();
					c8o_PagesImport += "import { "+pageName+" } from \"../pages/"+pageName+"/"+pageName.toLowerCase()+"\";\n";
					c8o_PagesLinks += " { component: "+pageName+", name: \""+pageName+"\", segment: \""+pageSegment+"\" }" + (isLastPage ? "":",");
					c8o_PagesDeclarations += " " + pageName + (isLastPage ? "":",");
					i++;
				}
				
				File appModuleTpl = new File(ionicTplDir, "src/app/app.module.ts");
				String mContent = FileUtils.readFileToString(appModuleTpl, "UTF-8");
				mContent = mContent.replaceAll("/\\*\\=c8o_PagesImport\\*/",c8o_PagesImport);
				mContent = mContent.replaceAll("/\\*\\=c8o_PagesLinks\\*/",c8o_PagesLinks);
				mContent = mContent.replaceAll("/\\*\\=c8o_PagesDeclarations\\*/",c8o_PagesDeclarations);
				File appModuleTsFile = new File(ionicWorkDir, "src/app/app.module.ts");
				FileUtils.write(appModuleTsFile, mContent, "UTF-8");
				
				Engine.logEngine.debug("(MobileBuilder) Ionic module ts file generated for 'app'");
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write ionic app module ts file",e);
		}
	}
	
	private void writeAppComponentTs(ApplicationComponent app) throws EngineException {
		try {
			if (app != null) {
				String c8o_PagesImport = "";
				String c8o_PagesVariables = "";
				String c8o_RootPage = "null";
				String c8o_AppComponentMarkers = app.getComponentScriptContent().getString();
				int i=1;
				
				
				List<PageComponent> pages = getEnabledPages(app);
				for (PageComponent page : pages) {
					String pageName = page.getName();
					String pageTitle = page.getTitle();
					boolean isRootPage = page.isRoot;
					boolean isMenuPage = page.isInAutoMenu();
					boolean isLastPage = i == pages.size();
					if (isRootPage) c8o_RootPage = pageName;
					c8o_PagesImport += "import { "+pageName+" } from \"../pages/"+pageName+"/"+pageName.toLowerCase()+"\";\n";
					c8o_PagesVariables += " { title: \""+pageTitle+"\", component: "+pageName+", includedInAutoMenu: "+ isMenuPage+"}" + (isLastPage ? "":",");
					i++;
				}
				
				String computedRoute = app.getComputedRoute();
				File appComponentTpl = new File(ionicTplDir, "src/app/app.component.ts");
				String cContent = FileUtils.readFileToString(appComponentTpl, "UTF-8");
				cContent = cContent.replaceAll("/\\*\\=c8o_PagesImport\\*/",c8o_PagesImport);
				cContent = cContent.replaceAll("/\\*\\=c8o_RootPage\\*/",c8o_RootPage);
				cContent = cContent.replaceAll("/\\*\\=c8o_PagesVariables\\*/",c8o_PagesVariables);
				cContent = cContent.replaceAll("/\\*\\=c8o_RoutingTable\\*/",computedRoute);
				
				Pattern pattern = Pattern.compile("/\\*Begin_c8o_(.+)\\*/"); // begin c8o marker
				Matcher matcher = pattern.matcher(cContent);
				while (matcher.find()) {
					String markerId = matcher.group(1);
					String tplMarker = getMarker(cContent, markerId);
					String customMarker = getMarker(c8o_AppComponentMarkers, markerId);
					if (!customMarker.isEmpty()) {
						cContent = cContent.replace(tplMarker, customMarker);
					}
				}
				
				File appComponentTsFile = new File(ionicWorkDir, "src/app/app.component.ts");
				FileUtils.write(appComponentTsFile, cContent, "UTF-8");
				
				Engine.logEngine.debug("(MobileBuilder) Ionic component ts file generated for 'app'");
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write ionic app component ts file",e);
		}
	}
	
	private void writeAppComponentTempTs(ApplicationComponent app) throws EngineException {
		try {
			if (app != null) {
				File appTsFile = new File(ionicWorkDir, "src/app/app.component.ts");
				File tempTsFile = new File(ionicWorkDir, "src/app/app.component.temp.ts");
				
				FileUtils.copyFile(appTsFile, tempTsFile);
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write ionic app component temp ts file",e);
		}
	}
	
	private void writeAppTemplate(ApplicationComponent app) throws EngineException {
		try {
			if (app != null) {
				String appName = app.getName();
				File appHtmlFile = new File(ionicWorkDir, "src/app/app.html");
				String computedTemplate = app.getComputedTemplate();
				FileUtils.write(appHtmlFile, computedTemplate, "UTF-8");
				Engine.logEngine.debug("(MobileBuilder) Ionic template file generated for app '"+appName+"'");
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write ionic app html file",e);
		}
	}
	
	private void writeAppStyle(ApplicationComponent app) throws EngineException {
		try {
			if (app != null) {
				String appName = app.getName();
				File appScssFile = new File(ionicWorkDir, "src/app/app.scss");
				String computedScss = app.getComputedStyle();
				FileUtils.write(appScssFile, computedScss, "UTF-8");
				
				Engine.logEngine.debug("(MobileBuilder) Ionic scss file generated for app '"+appName+"'");
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write ionic app scss file",e);
		}
	}

	private void writeAppTheme(ApplicationComponent app) throws EngineException {
		try {
			if (app != null) {
				String appName = app.getName();
				File themeScssFile = new File(ionicWorkDir, "src/theme/variables.scss");
				String tContent = app.getComputedTheme();
				FileUtils.write(themeScssFile, tContent, "UTF-8");
				
				Engine.logEngine.debug("(MobileBuilder) Ionic theme scss file generated for app '"+appName+"'");
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write ionic theme scss file",e);
		}
	}

	private void removeUselessPage(String pageName) {
		File pageDir = new File(ionicWorkDir,"src/pages/"+ pageName);
		try {
			FileUtils.deleteDirectory(pageDir);
		}
		catch(Exception e) {}
	}
	
	private void removeUselessPages(ApplicationComponent application) {
		if (application != null) {
			File ionicPagesDir = new File(ionicWorkDir,"src/pages");
			List<String> pageDirectories = new ArrayList<String>();
			pageDirectories.add(ionicPagesDir.getAbsolutePath());
			
			List<PageComponent> pages = application.getPageComponentList();
			for (PageComponent page : pages) {
				File pageDir = new File(ionicPagesDir, page.getName());
				pageDirectories.add(pageDir.getAbsolutePath());
			}
			for (File dir: FileUtils.listFilesAndDirs(ionicPagesDir, FalseFileFilter.INSTANCE, DirectoryFileFilter.DIRECTORY)) {
				if (!pageDirectories.contains(dir.getAbsolutePath())) {
					try {
						FileUtils.deleteDirectory(dir);
					}
					catch(Exception e) {}
				}
			}
		}
	}
	
	private void writeAppSourceFiles(ApplicationComponent application) throws EngineException {
		try {
			if (application != null) {
				writeAppModuleTs(application);
				writeAppComponentTs(application);
				writeAppTemplate(application);
				writeAppStyle(application);
				writeAppTheme(application);

				Engine.logEngine.debug("(MobileBuilder) Ionic source files generated for application 'app'");
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write source files for application 'app'",e);
		}
	}
	
	private void writePageSourceFiles(PageComponent page) throws EngineException {
		String pageName = page.getName();
		try {
			File pageDir = new File(ionicWorkDir,"src/pages/"+pageName);
			pageDir.mkdirs();
			
			writePageTs(page);
			writePageStyle(page);
			writePageTemplate(page);
			
			Engine.logEngine.debug("(MobileBuilder) Ionic source files generated for page '"+pageName+"'");
		}
		catch (Exception e) {
			throw new EngineException("Unable to write source files for page '"+pageName+"'",e);
		}
	}
	
	public static String getMarkers(String content) {
		String markers = "";
		Pattern pattern = Pattern.compile("/\\*Begin_c8o_(.+)\\*/"); // begin c8o marker
		Matcher matcher = pattern.matcher(content);
		while (matcher.find()) {
			String markerId = matcher.group(1);
			String marker = getMarker(content, markerId);
			if (!marker.isEmpty()) {
				markers += marker + System.lineSeparator();
			}
		}
		return markers;
	}
	
	public static String getMarker(String s, String markerId) {
		String beginMarker = "/*Begin_c8o_" + markerId + "*/";
		String endMarker = "/*End_c8o_" + markerId + "*/";
		int beginIndex = s.indexOf(beginMarker);
		if (beginIndex != -1) {
			int endIndex = s.indexOf(endMarker, beginIndex);
			if (endIndex != -1) {
				return s.substring(beginIndex, endIndex) + endMarker;
			}
		}
		return "";
	}
}
