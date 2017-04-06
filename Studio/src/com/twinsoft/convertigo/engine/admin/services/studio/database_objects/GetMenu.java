package com.twinsoft.convertigo.engine.admin.services.studio.database_objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Criteria;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.ExtractionRule;
import com.twinsoft.convertigo.beans.core.Listener;
import com.twinsoft.convertigo.beans.core.MobileApplication;
import com.twinsoft.convertigo.beans.core.MobileComponent;
import com.twinsoft.convertigo.beans.core.MobilePlatform;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Reference;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Sheet;
import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.TestCase;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.core.UrlMapper;
import com.twinsoft.convertigo.beans.core.UrlMapping;
import com.twinsoft.convertigo.beans.core.UrlMappingOperation;
import com.twinsoft.convertigo.beans.core.UrlMappingParameter;
import com.twinsoft.convertigo.beans.core.UrlMappingResponse;
import com.twinsoft.convertigo.beans.core.Variable;
import com.twinsoft.convertigo.beans.couchdb.DesignDocument;
import com.twinsoft.convertigo.beans.couchdb.FullSyncListener;
import com.twinsoft.convertigo.beans.mobile.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.mobile.components.PageComponent;
import com.twinsoft.convertigo.beans.mobile.components.RouteActionComponent;
import com.twinsoft.convertigo.beans.mobile.components.RouteComponent;
import com.twinsoft.convertigo.beans.mobile.components.RouteEventComponent;
import com.twinsoft.convertigo.beans.mobile.components.UIComponent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.NamedSourceSelector;
import com.twinsoft.convertigo.engine.ActionModel;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.DatabaseObjectsAction;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.util.TwsCachedXPathAPI;
import com.twinsoft.convertigo.engine.util.XMLUtils;


@ServiceDefinition(
		name = "GetMenu",
		roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG },
		parameters = {},
		returnValue = ""
	)
public class GetMenu extends XmlService {

	private TwsCachedXPathAPI xpathApi = new TwsCachedXPathAPI();
	private static Document pluginDocument = null;

	// Get the label of a menu from its ID
	private static Map<String, String> idMenuToLabel = new HashMap<>(3);
	static {
		idMenuToLabel.put("new", "New");
		idMenuToLabel.put("changeTo", "Change to");
		idMenuToLabel.put("cordova", "Cordova");
	}

	// Get the value of a folder from its name
	private static Map<String, String> folderNameToTypeValue = new HashMap<>(24);
	static {
		folderNameToTypeValue.put("Listener", "16");
		folderNameToTypeValue.put("Transaction", "0");
		folderNameToTypeValue.put("Handler", "2");
		folderNameToTypeValue.put("InheritedScreenClass", "5");
		folderNameToTypeValue.put("Sheet", "1");
		folderNameToTypeValue.put("Pool", "8");
		folderNameToTypeValue.put("ExtractionRule", "7");
		folderNameToTypeValue.put("Criteria", "6");
		folderNameToTypeValue.put("Connector", "9");
		folderNameToTypeValue.put("Sequence", "11");
		folderNameToTypeValue.put("Step", "12");
		folderNameToTypeValue.put("TestCase", "13");
		folderNameToTypeValue.put("Variable", "3");
		folderNameToTypeValue.put("Reference", "14");
		folderNameToTypeValue.put("Document", "15");
		folderNameToTypeValue.put("UrlMapping", "17");
		folderNameToTypeValue.put("UrlMappingOperation", "18");
		folderNameToTypeValue.put("UrlMappingParameter", "19");
		folderNameToTypeValue.put("UrlMappingResponse", "20");
		folderNameToTypeValue.put("MobilePlatform", "21");
		folderNameToTypeValue.put("Action", "25");
		folderNameToTypeValue.put("Event", "22");
		folderNameToTypeValue.put("Route", "23");
		folderNameToTypeValue.put("Page", "24");
	}
	
	// Get the model Class from the class name of its relating View
	private static Map<String, Class<?>> treeObjectToClass = new HashMap<>(33);
	static {
		treeObjectToClass.put("com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ConnectorTreeObject", Connector.class);
		treeObjectToClass.put("com.twinsoft.convertigo.eclipse.views.projectexplorer.model.CriteriaTreeObject", Criteria.class);
		treeObjectToClass.put("com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject", DatabaseObject.class);
//		treeObjectToClass.put("com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DesignDocumentFilterTreeObject", DesignDocumentFilter.class);
//		treeObjectToClass.put("com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DesignDocumentFunctionTreeObject", DesignDocumentFunction.class);
		treeObjectToClass.put("com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DesignDocumentTreeObject", DesignDocument.class);
//		treeObjectToClass.put("com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DesignDocumentUpdateTreeObject", DesignDocumentUpdate.class);
//		treeObjectToClass.put("com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DesignDocumentViewTreeObject", DesignDocumentView.class);
		treeObjectToClass.put("com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DocumentTreeObject", Document.class);
		treeObjectToClass.put("com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ExtractionRuleTreeObject", ExtractionRule.class);
//		treeObjectToClass.put("com.twinsoft.convertigo.eclipse.views.projectexplorer.model.FolderTreeObject", Folder.class);
		treeObjectToClass.put("com.twinsoft.convertigo.eclipse.views.projectexplorer.model.FullSyncListenerTreeObject", FullSyncListener.class);
//		treeObjectToClass.put("com.twinsoft.convertigo.eclipse.views.projectexplorer.model.HandlersDeclarationTreeObject", HandlersDeclaration.class);
		treeObjectToClass.put("com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ListenerTreeObject", Listener.class);
		treeObjectToClass.put("com.twinsoft.convertigo.eclipse.views.projectexplorer.model.MobileApplicationComponentTreeObject", ApplicationComponent.class);
		treeObjectToClass.put("com.twinsoft.convertigo.eclipse.views.projectexplorer.model.MobileApplicationTreeObject", MobileApplication.class);
		treeObjectToClass.put("com.twinsoft.convertigo.eclipse.views.projectexplorer.model.MobileComponentTreeObject", MobileComponent.class);
		treeObjectToClass.put("com.twinsoft.convertigo.eclipse.views.projectexplorer.model.MobilePageComponentTreeObject", PageComponent.class);
		treeObjectToClass.put("com.twinsoft.convertigo.eclipse.views.projectexplorer.model.MobilePlatformTreeObject", MobilePlatform.class);
		treeObjectToClass.put("com.twinsoft.convertigo.eclipse.views.projectexplorer.model.MobileRouteActionComponentTreeObject", RouteActionComponent.class);
		treeObjectToClass.put("com.twinsoft.convertigo.eclipse.views.projectexplorer.model.MobileRouteComponentTreeObject", RouteComponent.class);
		treeObjectToClass.put("com.twinsoft.convertigo.eclipse.views.projectexplorer.model.MobileRouteEventComponentTreeObject", RouteEventComponent.class);
		treeObjectToClass.put("com.twinsoft.convertigo.eclipse.views.projectexplorer.model.MobileUIComponentTreeObject", UIComponent.class);
		treeObjectToClass.put("com.twinsoft.convertigo.eclipse.views.projectexplorer.model.NamedSourceSelector", NamedSourceSelector.class);
		treeObjectToClass.put("com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ProjectTreeObject", Project.class);
//		treeObjectToClass.put("com.twinsoft.convertigo.eclipse.views.projectexplorer.model.PropertyTableColumnTreeObject", PropertyTableColumn.class);
//		treeObjectToClass.put("com.twinsoft.convertigo.eclipse.views.projectexplorer.model.PropertyTableRowTreeObject", PropertyTableRow.class);
//		treeObjectToClass.put("com.twinsoft.convertigo.eclipse.views.projectexplorer.model.PropertyTableTreeObject", PropertyTable.class);
		treeObjectToClass.put("com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ReferenceTreeObject", Reference.class);
//		treeObjectToClass.put("com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ResourceFolderTreeObject", ResourceFolder.class);
//		treeObjectToClass.put("com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ResourceTreeObject", Resource.class);
//		treeObjectToClass.put("com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ResourceXslTreeObject", ResourceXsl.class);
		treeObjectToClass.put("com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ScreenClassTreeObject", ScreenClass.class);
		treeObjectToClass.put("com.twinsoft.convertigo.eclipse.views.projectexplorer.model.SequenceTreeObject", Sequence.class);
		treeObjectToClass.put("com.twinsoft.convertigo.eclipse.views.projectexplorer.model.SheetTreeObject", Sheet.class);
		treeObjectToClass.put("com.twinsoft.convertigo.eclipse.views.projectexplorer.model.StatementTreeObject", Statement.class);
		treeObjectToClass.put("com.twinsoft.convertigo.eclipse.views.projectexplorer.model.StepTreeObject", Step.class);
		treeObjectToClass.put("com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TestCaseTreeObject", TestCase.class);
		treeObjectToClass.put("com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TransactionTreeObject", Transaction.class);
//		treeObjectToClass.put("com.twinsoft.convertigo.eclipse.views.projectexplorer.model.UnloadedProjectTreeObject", UnloadedProject.class);
		treeObjectToClass.put("com.twinsoft.convertigo.eclipse.views.projectexplorer.model.UrlMapperTreeObject", UrlMapper.class);
		treeObjectToClass.put("com.twinsoft.convertigo.eclipse.views.projectexplorer.model.UrlMappingOperationTreeObject", UrlMappingOperation.class);
		treeObjectToClass.put("com.twinsoft.convertigo.eclipse.views.projectexplorer.model.UrlMappingParameterTreeObject", UrlMappingParameter.class);
		treeObjectToClass.put("com.twinsoft.convertigo.eclipse.views.projectexplorer.model.UrlMappingResponseTreeObject", UrlMappingResponse.class);
		treeObjectToClass.put("com.twinsoft.convertigo.eclipse.views.projectexplorer.model.UrlMappingTreeObject", UrlMapping.class);
//		treeObjectToClass.put("com.twinsoft.convertigo.eclipse.views.projectexplorer.model.VariableTreeObject", Variable.class);
		treeObjectToClass.put("com.twinsoft.convertigo.eclipse.views.projectexplorer.model.VariableTreeObject2", Variable.class);
//		treeObjectToClass.put("com.twinsoft.convertigo.eclipse.views.projectexplorer.model.XMLRecordDescriptionRowTreeObject", XMLRecordDescriptionRow.class);
//		treeObjectToClass.put("com.twinsoft.convertigo.eclipse.views.projectexplorer.model.XMLRecordDescriptionTreeObject", XMLRecordDescription.class);
//		treeObjectToClass.put("com.twinsoft.convertigo.eclipse.views.projectexplorer.model.XMLTableDescriptionColumnTreeObject", XMLTableDescriptionColumn.class);
//		treeObjectToClass.put("com.twinsoft.convertigo.eclipse.views.projectexplorer.model.XMLTableDescriptionRowTreeObject", XMLTableDescriptionRow.class);
//		treeObjectToClass.put("com.twinsoft.convertigo.eclipse.views.projectexplorer.model.XMLTableDescriptionTreeObject", XMLTableDescription.class);		
	}

	@Override
	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		// Load the plugin.xml where the representation of the context menu is described
		if (pluginDocument == null) {
			pluginDocument = XMLUtils.loadXml("C:/dev/Eclipses/projects/CemsStudio_7.5.x/plugin.xml");
		}
		
		String[] qnames = request.getParameterValues("qnames[]");
		String[] folderTypes = request.getParameterValues("folderTypes[]");

		// Create the response : success or fail
		Element eResponse = document.createElement("response");
		Element eRoot = document.getDocumentElement();
		eRoot.appendChild(eResponse);
		
		// Select (folder + database object) or (multiple folders) = no menu generated
		if (folderTypes != null && (qnames != null || folderTypes.length > 1)) {
			// It is not really an error but it's ok
			noEntryMessage(eResponse);
			return;
		}
		
		Element eContextMenu = null;
		// Generate the menu for database objects
		if (qnames != null) {
			List<Node> menus = new ArrayList<>(qnames.length);

			for (String qname: qnames) {				
				// Create menu for the dbo
				DatabaseObject dbo = Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(qname);
				if (dbo != null) {
					Element menu = createDboMenu(dbo, document);
					menus.add(menu);
				}
				else {
					eResponse.setAttribute("state", "error");
					eResponse.setAttribute("message", "One of the qname is invalid.");
					return;
				}
			}
			
			// Generate the new menu with all generated menus
			eContextMenu = generateFilteredMenu(document, menus);
		}
		// Generate the menu for folders
		else if (folderTypes != null) {
			eContextMenu = document.createElement("menu");
			String folderTypeValue = folderNameToTypeValue.get(folderTypes[0]);
			if (folderTypeValue != null) {		
				// Get the object contribution related to the folder
				Element eObjectContribution = (Element) xpathApi.selectNode(pluginDocument, "/plugin/extension[@point='org.eclipse.ui.popupMenus']/objectContribution[@objectClass='com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ObjectsFolderTreeObject']");
				
				// Search the action of the folder
				List<Node> nActions = xpathApi.selectList(eObjectContribution, "/*");
				boolean foundAction = false;
				for (int i = 0; !foundAction && i < nActions.size(); ++i) {
					Element eAction = (Element) nActions.get(i);
					
					/*
					 *  If we have the right action, we add it to the menu and stop looping.
					 *  In the Studio, we display other actions as disabled but here we only
					 *  display the specific action. In face we can only have one action
					 *  so it is useless to display the others.
					 */
					if (foundAction = evaluateFolderCondition(folderTypeValue, eAction)) {
						// Create the action
						Element eNewAction = document.createElement("action");	
						eNewAction.setAttribute("isEnabled", "true");
						eNewAction.setAttribute("label", eAction.getAttribute("label"));
						eNewAction.setAttribute("class", eAction.getAttribute("class"));
						
						// Create the sub menu
						String menubarPath = eAction.getAttribute("menubarPath");
						eNewAction.setAttribute("menubarPath", menubarPath);
						int index = menubarPath.indexOf("/");
						createSubMenu(menubarPath, index, document, eContextMenu, eNewAction, eObjectContribution.getAttribute("id"));		
					}
				}
			}
		}
		
		// Create response
		if (eContextMenu.hasChildNodes()) {
			eResponse.setAttribute("state", "success");
			eResponse.setAttribute("message", "Context menu has been generated.");
			eRoot.appendChild(eContextMenu);
		}
		else {
			noEntryMessage(eResponse);
		}
	}

	private void noEntryMessage(Element response) {
		response.setAttribute("state", "error");
		response.setAttribute("message", "Context menu has no entry.");
	}
	
	private Element generateFilteredMenu(Document document, List<Node> menus) {
		// No need to filter if we have only one menu
		if (menus.size() == 1) {
			return (Element) menus.get(0);
		}
		
		/*
		 *  The new filtered menu : the goal is to look through the menus
		 *  and only keep similar entries (= same ID).
		 */
		Element eFilteredMenu = document.createElement("menu");
		
		// Get the first menu of the list as the referent
		Element eReferentMenu = (Element) menus.get(0);
		
		// Get the action/menu entries
		List<Node> nEntries = xpathApi.selectList(eReferentMenu, "*");
		
		// We will check if all entries of the referent menu are also in the other menus
		for (Node nEntry: nEntries) {
			// Current entry
			Element eCurrentEntry = (Element) nEntry;
			
			// Flags to now if what we will to with this entry
			boolean isEntryPresentInOtherMenus = true;
			boolean mustCheckIsEnabled = true;
			boolean mustCheckIsChecked = true;

			// Check if the current entry exits in all other menus
			for (int i = 1; isEntryPresentInOtherMenus && i < menus.size(); ++i) {
				Node nEntry2 = xpathApi.selectNode(menus.get(i), "/*[@id='" + eCurrentEntry.getAttribute("id") + "']");
				if (isEntryPresentInOtherMenus = nEntry2 != null) {
					Element eEntry2 = (Element) nEntry2;
					
					// Disable the entry if needed
					if (mustCheckIsEnabled && eEntry2.getAttribute("isEnabled").equals("false")) {
						mustCheckIsEnabled = false;
						eCurrentEntry.setAttribute("isEnabled", "false");
					}
					
					// Unchecked the entry if needed
					if (mustCheckIsChecked && eEntry2.getAttribute("isChecked").equals("false")) {
						mustCheckIsChecked = false;
						eCurrentEntry.setAttribute("isChecked", "false");
					}
				}
			}
			
			if (isEntryPresentInOtherMenus) {
				// If the entry is a menu
				if (eCurrentEntry.getNodeName().equals("menu")) {
					List<Node> nChildren2 = xpathApi.selectList(eCurrentEntry, "*");
					for (Node nChild2: nChildren2) {
						Element eChild2 = (Element) nChild2;
						
						// Disable all entry
						if (eChild2.getAttribute("enablesFor").equals("1")) {
							eChild2.setAttribute("isEnabled", "false");
						}
					}
				}
				else {
					// Disable the current entry
					if (eCurrentEntry.getAttribute("enablesFor").equals("1")) {
						eCurrentEntry.setAttribute("isEnabled", "false");
					}
				}

				eFilteredMenu.appendChild(eCurrentEntry);
			}
		}
		
		return eFilteredMenu;
	}
	
	private Element createDboMenu(DatabaseObject dbo, Document document) throws ClassNotFoundException {
		Element menuRootElt = document.createElement("menu");
		
		// Get nodes object contribution
		for (Node nObjectContribution: xpathApi.selectList(pluginDocument, "/plugin/extension[@point='org.eclipse.ui.popupMenus']/objectContribution[@objectClass]")) {
			String attrObjectClass = ((Element) nObjectContribution).getAttribute("objectClass");
			if (testObjectClass(dbo, attrObjectClass)) {
				boolean isObjectContributionVisible = evaluateVisibilityCondition(dbo, nObjectContribution);
				if (isObjectContributionVisible) {
					// Get nodes action
					for (Node nAction: xpathApi.selectList(nObjectContribution, "/action")) {
						Element eAction = (Element) nAction;
						
						// Create the new action
						Element eNewAction = document.createElement("action");						
						ActionModel actionModel = evaluateEnablementCondition(dbo, nAction);
						eNewAction.setAttribute("isEnabled", Boolean.toString(actionModel.isEnabled));
						eNewAction.setAttribute("objectClass", attrObjectClass);
						eNewAction.setAttribute("label", actionModel.text != null ? actionModel.text : eAction.getAttribute("label"));
						eNewAction.setAttribute("isChecked", Boolean.toString(actionModel.isChecked));
						eNewAction.setAttribute("class", eAction.getAttribute("class"));
						eNewAction.setAttribute("id", eAction.getAttribute("id"));
						eNewAction.setAttribute("enablesFor", eAction.getAttribute("enablesFor"));

						// menubarPath = category
						String menubarPath = eAction.getAttribute("menubarPath");
						eNewAction.setAttribute("menubarPath", menubarPath);
						
						int index = menubarPath.indexOf("/");
						// Case of a sub-menu
						if (index != -1) {
							createSubMenu(menubarPath, index, document, menuRootElt, eNewAction, ((Element) nObjectContribution).getAttribute("id"));
						}
						else {
							addActionElt(menuRootElt, eNewAction, attrObjectClass, menubarPath);
						}
					}
				}
			}
		}
		
		return menuRootElt;
	}
	
	private void createSubMenu(String menubarPath, int index, Document document, Element menuRootElt, Element eNewAction, String objectContributionId) {
		String subMenuLabel = menubarPath.substring(menubarPath.lastIndexOf(".") + 1, index);
		String subMenuId = objectContributionId + "." + subMenuLabel;
		subMenuLabel = idMenuToLabel.get(subMenuLabel);
		Element subMenuElt = (Element) xpathApi.selectNode(menuRootElt, "/menu[@id='" + subMenuId + "']");
		
		// Create the sub menu if it does not exist
		if (subMenuElt == null) {
			subMenuElt = document.createElement("menu");
			subMenuElt.setAttribute("label", subMenuLabel);

			subMenuElt.setAttribute("id", subMenuId);
			menuRootElt.insertBefore(subMenuElt, menuRootElt.getFirstChild());
		}
		
		addActionElt(subMenuElt, eNewAction, null, menubarPath);
	}
	
	private void addActionElt(Element root, Element action, String attrObjectClass, String menubarPath) {
		Element actionObjectClass = attrObjectClass == null ? null : (Element) xpathApi.selectNode(root, "(/action[@objectClass='" + attrObjectClass + "'])[last()]");
		if (actionObjectClass == null) {
			Node nLastSubMenu = xpathApi.selectNode(root, "(menu)[last()]");
			root.insertBefore(action, nLastSubMenu != null ?
					// Insert action after the last sub-menu
				    nLastSubMenu.getNextSibling() :
				    // Insert action at the beginning
				    root.getFirstChild());
		}
		else {
			Element current = (Element) xpathApi.selectNode(root, "/action[@menubarPath='" + menubarPath + "']");
			root.insertBefore(action, current == null ? actionObjectClass.getNextSibling() : current);
		}
	}
	
	private boolean evaluateFolderCondition(String folderTypeValue, Node node) {
		Element eObjectState = (Element) xpathApi.selectNode(node, "*/*");
		String attrValue = eObjectState.getAttribute("value");
		return attrValue.equals(folderTypeValue);
	}
	
	private boolean evaluateVisibilityCondition(DatabaseObject dbo, Node node) throws ClassNotFoundException {
		boolean condition = true;
		
		// Evaluate the tree expression
		Node nCondition = xpathApi.selectNode(node, "*/*");
		if (nCondition != null) {
			condition = evaluateCondition(dbo, nCondition);
		}
		
		return condition;
	}
	
	private ActionModel evaluateEnablementCondition(DatabaseObject dbo, Node node) throws ClassNotFoundException {
		boolean condition = evaluateVisibilityCondition(dbo, node);
		
		ActionModel actionModel = DatabaseObjectsAction.selectionChanged(((Element) node).getAttribute("class"), dbo);
		if (actionModel.isEnabled != null) {
			if (!actionModel.isEnabled) {
				actionModel.isEnabled = condition && actionModel.isEnabled;
			}
		}
		else {
			actionModel.isEnabled = condition;
		}
		
		return actionModel;
	}
	
	private boolean evaluateCondition(DatabaseObject dbo, Node node) throws ClassNotFoundException {		
		Element condition = (Element) node;
		String tCondition = condition.getNodeName();
		boolean result = true;
		
		if (tCondition.equals("objectClass")) {
			result = testObjectClass(dbo, condition.getAttribute("name"));
		}
		else if (tCondition.equals("objectState")) {
			String name = condition.getAttribute("name");
			String value = condition.getAttribute("value");
			result = name.equals("objectClassName") ?
					testObjectStateObjectClassNameAttribute(dbo, value.split(";")) :
				    dbo.testAttribute(name, value);
		}
		else if (tCondition.equals("and")) {
			boolean isOk = true;
			for (Node n: xpathApi.selectList(condition, "*")) {
				isOk &= evaluateCondition(dbo, n);
				if (!isOk) {
					break;
				}
			}
			result = isOk;
		}
		else if (tCondition.equals("or")) {
			boolean isOk = false;
			for (Node n: xpathApi.selectList(condition, "*")) {
				isOk |= evaluateCondition(dbo, n);
				if (isOk) {
					break;
				}
			}
			result = isOk;
		}
		else if (tCondition.equals("not")) {
			result = !evaluateCondition(dbo, xpathApi.selectNode(condition, "*"));
		}
		
		return result;
	}
	
	private boolean testObjectStateObjectClassNameAttribute(DatabaseObject dbo, String ...objectClassNameValues) throws ClassNotFoundException {
		for (String objectClassNameValue: objectClassNameValues) {
			if (objectClassNameValue.equals(dbo.getClass().getName())) {
				return true;
			}
		}
		
		return false;
	}
	
	private boolean testObjectClass(DatabaseObject dbo, String objectClassValue) {
		Class<?> contributionClass = treeObjectToClass.get(objectClassValue);
		return contributionClass != null && contributionClass.isAssignableFrom(dbo.getClass());
	}

}
