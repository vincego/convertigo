var Main = {
	init: function (authUserName, authPassword) {
		this.defineScripts();
		
		// Require order is important
		require([
			/**
			 * Libs
			 */
			"jquery",
			"jstree",
			"jstreegrid",
			"jstreeutils",
			"jquery-ui",
			"jquery.modal",
			"accordion",

	    	/**
	    	 * Managers
	    	 */
	    	"database-object-manager",
	        "response-action-manager",

	        /**
	         * Tabs
	         */
	    	"tab",
	    	"palette",
	    	"references",
	    	"source-picker",
	    	"studio-tabs",

	        /**
	         * Utils
	         */
	        "injector-utils",
	        "modal-utils",
	        "string-utils",

	        /**
	         * Views
	         */
	        "information",
	        "projects"
		], function () {
			// Inject CSS
			InjectorUtils.injectLinkStyle(Convertigo.getBaseConvertigoStudioUrl("css/jquery/jstree/themes/default-dark/style.min.css"));
			InjectorUtils.injectLinkStyle(Convertigo.getBaseConvertigoStudioUrl("css/jquery/jquery-ui.min-1.12.1.css"));
			InjectorUtils.injectLinkStyle(Convertigo.getBaseConvertigoStudioUrl("css/jquery/jquery.modal.min-0.8.0.css"));
			InjectorUtils.injectLinkStyle(Convertigo.getBaseConvertigoStudioUrl("css/accordion.css"));
			InjectorUtils.injectLinkStyle(Convertigo.getBaseConvertigoStudioUrl("css/style.css"));

			// To iterate in reverse order
			jQuery.fn.reverse = [].reverse;

			// Define AJAX setup
			$.ajaxSetup({
				type: "POST",
				dataType: "xml",
				xhrFields: {
					withCredentials: true
				}
			});

			// Connect to the Convertigo server
			Convertigo.authenticate(authUserName, authPassword, function () {
				// Inject CSS that needed an authentifcation to the Convertigo server
				InjectorUtils.injectLinkStyle(Convertigo.createServiceUrl("studio.database_objects.GetMenuIconsCSS"));
				InjectorUtils.injectLinkStyle(Convertigo.createServiceUrl("studio.database_objects.GetPaletteIconsCSS"));
				InjectorUtils.injectLinkStyle(Convertigo.createServiceUrl("studio.database_objects.GetTreeIconsCSS"));

				// Will contain projects view + tabs
				var $projectsViewDiv = $(".projectsView");				

				// Will contain all tabs
				var studioTabs = new StudioTabs();

				// Create Source Picker tab
				var sourcePicker = new SourcePicker();
				studioTabs.addTab(sourcePicker);

				// Create References tab
				var references = new References();
				studioTabs.addTab(references);

				// Create Palette tab
				var palette = new Palette();
				studioTabs.addTab(palette);

				studioTabs.renderTabs();

				var projectsView = new ProjectsView([palette]);

				// Add projects tree view + Palette
				$projectsViewDiv
					.append(projectsView.getDivWrapperTree())
					.append($("<hr/>"))
					.append(studioTabs.getDiv());

				// Refresh projects tree view when pressing F5 (for debug prupose)
				$(document).on("keydown", function (e) {
					if ((e.which || e.keyCode) == 116) {
						e.preventDefault();
						projectsView.tree.jstree().refresh(true);
					}
				});

				// Properties view
				PropertiesView.init(".informationView");
				DatabaseObjectManager.addListener(projectsView);
				DatabaseObjectManager.addListener(PropertiesView);

				// Call check authentication to stay authenticated
				Convertigo.checkAuthentication();
			});
		});
	},
	defineScripts: function () {
		// All scripts are defined here
		require.config({
		    paths: {
		    	/**
		    	 * Libs
		    	 */
		        jquery: Convertigo.getBaseConvertigoUrl("scripts/jquery2.min"),
		        jstree: Convertigo.getBaseConvertigoStudioUrl("js/libs/jquery/jstree/jstree-3.3.3.min"),
		        jstreegrid: Convertigo.getBaseConvertigoStudioUrl("js/libs/jquery/jstree/jstreegrid-3.5.14"),
		        jstreeutils: Convertigo.getBaseConvertigoStudioUrl("js/libs/jquery/jstree/jstreeutils"),
		        "jquery-ui": Convertigo.getBaseConvertigoStudioUrl("js/libs/jquery/jquery-ui.min-1.12.1"),
		        "jquery.modal": Convertigo.getBaseConvertigoStudioUrl("js/libs/jquery/jquery.modal.min-0.8.0"),
		        accordion: Convertigo.getBaseConvertigoStudioUrl("js/libs/accordion"),

		    	/**
		    	 * Managers
		    	 */
		    	"database-object-manager": Convertigo.getBaseConvertigoStudioUrl("js/managers/database-object-manager"),
		        "response-action-manager": Convertigo.getBaseConvertigoStudioUrl("js/managers/response-action-manager"),

		        /**
		         * Tabs
		         */
		    	palette: Convertigo.getBaseConvertigoStudioUrl("js/tabs/palette"),
		    	references:  Convertigo.getBaseConvertigoStudioUrl("js/tabs/references"),
		    	"source-picker": Convertigo.getBaseConvertigoStudioUrl("js/tabs/source-picker"),
		    	"studio-tabs": Convertigo.getBaseConvertigoStudioUrl("js/tabs/studio-tabs"),
		    	tab: Convertigo.getBaseConvertigoStudioUrl("js/tabs/tab"),

		        /**
		         * Utils
		         */
		        "injector-utils": Convertigo.getBaseConvertigoStudioUrl("js/utils/injector-utils"),
		        "modal-utils": Convertigo.getBaseConvertigoStudioUrl("js/utils/modal-utils"),
		        "string-utils": Convertigo.getBaseConvertigoStudioUrl("js/utils/string-utils"),

		        /**
		         * Views
		         */
		        information: Convertigo.getBaseConvertigoStudioUrl("js/views/information"),
		        projects: Convertigo.getBaseConvertigoStudioUrl("js/views/projects")
		    },
		    // To resolve jQuery conflicts
		    shim: {
		        "jquery.modal": ["jquery"],
		        "jquery-ui": ["jquery"]
		    }
		});
	}
};

docReady(function () {
	var baseConvertigoUrl = "http://localhost:18080/";
	var authUserName = "admin";
	var authPassword = "admin";

	Convertigo.init(baseConvertigoUrl);
	Main.init(authUserName, authPassword);
});