//TODO: add a log function and use it!
C8O = {	
	vars : { /** customizable value */
		ajax_method : "POST", /** POST/GET */
		requester_prefix : ""
	},
		
	addHook : function (name, fn) {
		if ($.isFunction(fn)) {
			if (!$.isArray(C8O._define.hooks[name])) {
				C8O._define.hooks[name] = [];
			}
			C8O._define.hooks[name].push(fn);
		}
	},
	
	addRecallParameter : function (parameter_name, parameter_value) {
		if (C8O.isUndefined(parameter_value)) {
			parameter_value = "";
		}
		C8O._define.recall_params[parameter_name] = parameter_value;
	},
	
		
	call : function (data) {
		C8O._loadingStart();
		if (typeof(data) === "string") {
			data = C8O._parseQuery({}, data);
		} else if (C8O.isUndefined(data)) {
			data = {};
		} else if (!$.isPlainObject(data) && $(data).is("form")) {
			data = C8O._parseQuery({}, $(data).serialize());
		}
		
		for (key in C8O.vars) {
			if (!C8O.isUndefined(data["__" + key])) {
				C8O.vars[key] = C8O._remove(data, "__" + key);
			}
		}
		for (key in C8O._define.recall_params) {
			if (C8O._define.recall_params.hasOwnProperty(key)) {
				if (!C8O.isUndefined(data[key])) {
					C8O._define.recall_params[key] = data[key];
				}
				if (C8O._define.recall_params[key]) {
					data[key] = C8O._define.recall_params[key];
				} else {
					delete data[key];
				}
			}
		}
		
		C8O._call(data);
	},
	
	getLastCallParameter : function (key) {
		if (C8O.isUndefined(key)) {
			return C8O._obj_clone(C8O._define.last_call_params);
		} else {
			return C8O._define.last_call_params[key];
		}
	},
	
	isDefined : function (obj) {
		return typeof(obj) !== "undefined";
	},
	
	isUndefined : function (obj) {
		return typeof(obj) === "undefined";
	},
	
	removeRecallParameter : function (parameter_name) {
		delete C8O._define.recall_params[parameter_name];
	},
	
	_define : {
		hooks : {},
		last_call_params : {},
		recall_params : {__context : "", __connector : ""}
	},

	
	/********************************************************* WebSQL Offline Data implementation *****************************************************/  
	db: null,
	_WSqlinit: function () {
		if (window.openDatabase) {
			this.db = window.openDatabase("C8OOfflineData", "1.0", "Offline data for Convertigo FrameWork", 10000);
			if (this.db) {
				this.db.transaction(function(tx) {
					tx.executeSql("CREATE TABLE IF NOT EXISTS C8OCache (QS PRIMARY KEY, data BLOB)", [],
					        function(tx, rs) {
						
							},
					        function(tx, error) {
					            console.log("Error While creating the WSQL cache: " + error.message);
					            return;
					        }
					);
				});
			}
		}
	},

	_WSqlcleanCache: function () {
		if (this.db) {
			this.db.transaction(function(tx) {
				tx.executeSql("DROP TABLE C8OCache", [],
				        function(tx, rs) {
					
						},
				        function(tx, error) {
				            console.log("Error While cleaning the WSQL cache: " + error.message);
				            return;
				        }
				);
			});
		}
	},
	
	_WSqlcacheResponse: function(key, data) {
		if (this.db) {
			this.db.transaction(function(tx) {
				var xmlString = (new XMLSerializer()).serializeToString(data);
				tx.executeSql("INSERT  OR REPLACE INTO C8OCache (QS, data) values (?,?) ",
						[JSON.stringify(key), xmlString],
				        function(tx, rs) {
					
						},
				        function(tx, error) {
				            console.log("Error While setting data in WSL cache: " + error.message);
				            return;
				        }
				);
			});			
		}
	},

	_WSqlgetCachedData: function (keyObject, callDone) {
		if (this.db) {
			this.db.transaction(function(tx) {
				tx.executeSql("SELECT data FROM C8OCache WHERE QS = ?",
						[JSON.stringify(keyObject)],
				        function(tx, rs) {
							if (rs.rows.length === 1) {
								var XMLdata = rs.rows.item(0).data;
								var doc = $.parseXML(XMLdata);
								callDone(doc);
							} else {
								C8O._loadingStop();
								alert("Requested data is not available, please retry when connected");
							}
						},
						
				        function(tx, error) {
				            console.log("Error While getting data from WSL cache: " + error.message);
				            return;
				        }
				);
			});			
		}
	},
	
	/******************************** IndexedDB implementation *************************************************************************************/
	_DatabaseName: "C8OOfflineDataBase",
	_StoreName: "C8Ocache",
	
	_IxDbcleanCache: function() {
		var deletePromise = $.indexedDB(this._DatabaseName).deleteDatabase();
		
		deletePromise.done(function( error, event) { 
			console.log("Database deleted: " + event.type);
		});
		
		deletePromise.fail(function(error, event){ 
			console.log("Error when delete Database: " + error);
		});
		
		deletePromise.progress(function(db, event){ 
			console.log("delete database in progress: " + event.type);
		});
	},
	
	_IxDbcacheResponse: function(key, data) {
		if (this.db) {
			var transPromise = $.indexedDB(C8O._DatabaseName).transaction(C8O._StoreName, "readwrite");
			transPromise.progress(function (trans) {
				var oStore = trans.objectStore(C8O._StoreName);
				var xmlString = (new XMLSerializer()).serializeToString(data);
				var promise = oStore.put(xmlString, JSON.stringify(key));
				
				promise.done(function(result, event) {
					console.log("response cached: " + event.type);
				});
				
				promise.fail(function(error, event) {
					console.log("response failed to cache: " + error);
				});
			});
			
			transPromise.fail(function (error, event){
				console.log("Transaction failed: " + error);
			});
			
			transPromise.done(function (event){
				console.log("Transaction done: " + event);
			});
			
		}
	},
	
	_IxDbinit: function () {
		var dbPromise = $.indexedDB(this._DatabaseName, {
			"version": 1,
			"schema": {
				"1": function(transaction) {
					var Store = transaction.createObjectStore(C8O._StoreName);
					console.log("Store created: " + Store);
				}
			}
		});
		
		dbPromise.fail(function(error, event) {
			console.log("error creating database: " + error);
			console.log("event creating database: " + event);
		});
		
		dbPromise.progress(function(error, event) {
			// console.log("Open in progress, error : " + error);
			console.log("Open in progress : " + event.type);
		});
		
		dbPromise.done(function(db, event) {
			console.log("event : " + event.type);
			console.log("db : " + db);
			C8O.db = db;
		});	
	},

	_IxDbgetCachedData: function (keyObject, callDone) {
		if (this.db) {
			var transPromise = $.indexedDB(C8O._DatabaseName).transaction(C8O._StoreName, "readwrite");
			transPromise.progress(function (trans) {
				var oStore = trans.objectStore(C8O._StoreName);
				var key = JSON.stringify(keyObject);
				console.log("getting data with key:" + key);
				var promise = oStore.get(key);
				promise.done(function(resObject, event) {
					console.log("data retreived from cache: " + event.type);
					console.log("data is :" + resObject);
					if (C8O.isDefined(resObject)) {
						var doc = $.parseXML(resObject);
						callDone(doc);
					} else {
						C8O._loadingStop();
						alert("Requested data is not available, please retry when connected");
					}
				});
				
				promise.fail(function(error, event) {
					console.log("failed to retrieve data form cache: " + error);
				});
			});
			
			transPromise.fail(function (error, event){
				console.log("Get CachedData Transaction failed: " + error);
			});
			
			transPromise.done(function (event){
				console.log("Get CachedData Transaction done: " + event);
			});
			
		}
	},
	/****************End of Indexed DB Implementation *************************************************/
	_cleanCache: function() {
		switch (C8O.cacheMode) {
			case C8O.IxDb:   return (C8O._IxDbcleanCache());
			case C8O.WebSql: return (C8O._WSqlcleanCache());
		}
	},
	
	_init: function () {
		switch (C8O.cacheMode) {
			case C8O.IxDdb:  return (C8O._IxDbinit());
			case C8O.WebSql: return (C8O._WSqlinit());
		}
	},
	
	_getCachedData: function (keyObject, callDone) {
		switch (C8O.cacheMode) {
			case C8O.IxDdb:  return (C8O._IxDbgetCachedData(keyObject, callDone));
			case C8O.WebSql: return (C8O._WSqlgetCachedData(keyObject, callDone));
		}
	},
	
	_cacheResponse: function(key, data) {
		switch (C8O.cacheMode) {
			case C8O.IxDdb:  return (C8O._IxDbcacheResponse(key, data));
			case C8O.WebSql: return (C8O._WSqlcacheResponse(key, data));
		}
	},
	
	IxDb: "IndexedDB",
	WebSql : "WebSQL",
	cacheMode : "WebSQL",
	
	/*******************************************************************************************************/
	
	_call : function (data, bCache) {
		C8O._define.last_call_params = data;
		if (C8O._hook("call", data)) {
			var bCacheResponse = C8O.isDefined(bCache)? bCache: false;
			if (C8O.isConnected()) {
				$.ajax({
					data : data,
					dataType : "xml",
					success : function (xml, status, xhr) {
						if (C8O._hook("xml_response", xml, data)) {
							C8O._loadingStop();
							if(bCacheResponse);
								C8O._cacheResponse(data, xml);
						}
					},
					
					error: function (jqXHR, textStatus, errorThrown) {
						/**
						 * todo: manage error hooks to enable users to provide their own error handling
						 * routines
						 */
						C8O._loadingStop();
						alert($(jqXHR.responseXML).find("message").text());
					},
					
					type : C8O.vars.ajax_method,
					url : "../../" + C8O.vars.requester_prefix + ".xml"
				});
			} else {
				// retrieve data from cache
				C8O._getCachedData(data, function (xml) {
					if (C8O._hook("xml_response", xml)) {
						C8O._loadingStop();
					}
				});
			}
		}
	},
	
	_hook : function (name) {
		var ret = true, i, r;
		if ($.isArray(C8O._define.hooks[name])) {
			for (i = 0;i < C8O._define.hooks[name].length && ret;i += 1) {
				r = C8O._define.hooks[name][i].apply(this, $.makeArray(arguments).slice(1));
				if (!C8O.isUndefined(r)) {
					ret = r;
				}
			}
		}
		return ret;
	},
	
	_loadingStart : function () {
		if (C8O._hook("loading_start")) {
			$("#c8oloading").show();
			$.mobile.showPageLoadingMsg();	
		}
	},
	
	_loadingStop : function () {
		if (C8O._hook("loading_stop")) {
			$.mobile.hidePageLoadingMsg();
			$("#c8oloading").hide();
		}
	},
	
	_parseQuery : function (params, query) {
		var data = (C8O.isUndefined(params)) ? {}:params,
			vars = (query?query:C8O._getQuery()).split("&"),
			i, id, key, value;
		for (i = 0;i < vars.length; i += 1) {
			if (vars[i].length > 0) {
				id = vars[i].indexOf("=");
				key = (id > 0)?vars[i].substring(0, id):vars[i];
				value = "";
				if (id > 0) {
					try {
						value = decodeURIComponent(vars[i].substring(id + 1));
					}catch (err1) {
						try {
							value = unescape(vars[i].substring(id + 1));
						}catch (err2) {}
					}
				}
				if (C8O.isUndefined(data[key])) {
					data[key] = value;
				} else if ($.isArray(data[key])) {
					data[key].push(value);
				} else {
					data[key] = [data[key]].concat([value]);
				}
			}
		}
		return data;
	},
	
	_remove : function (object, attribute) {
		var res = object[attribute];
		delete object[attribute];
		return res;
	},
	
/***************************************************************************************************************
 * Convertigo MVC Framework additions.... 
 ***************************************************************************************************************/
	
	/**
	 * isConnected flag
	 */

	bConnected : true,
	
	isConnected: function() {
		return this.bConnected;
	},

	setConnected: function(status) {
		this.bConnected = status;
	},

	
	/**
	 * Reg exp selector for templating engine
	 */
	re : {
		selector : new RegExp("{(.*?)(?:@(.*?))?}"),
		accolade : new RegExp("{.*?(?!\\\\).}"),
		replaceSimpleQuote : new RegExp("(?:^|((?!\\\\).))'","g"), // replace with "$1\""
		removeBackslash : new RegExp("(\\\\*)\\\\","g"), // replace with "$1"
	},
	
	/**
	 * This will analyze C8O responses and route them to the correct page according to the
	 * routingTable. 
	 * 
	 * When the correct page is found we switch to this page and wait for the pageinit event to
	 * render all the widgets listening to this response.
	 * 
	 * If no page has to be switched, we render on the same page immediately
	 */
	routeResponse: function(c8oRequestable, xml) {
		var $doc = $(xml.documentElement);
		var routeFound = false;
		
		for (var i in C8O.routingTable) {
			var entry = C8O.routingTable[i];
			if (c8oRequestable == entry.calledRequest) {
				for (var j in entry.actions) {
					var action = entry.actions[j];
					var transition =  action.transition;
					
					if (typeof action.condition === "function") {
						routeFound = action.condition($doc);
					} else {
						var element = $doc.find(action.condition);
						routeFound = (element.length != 0);
					}

					if (routeFound) {
						var goToPage = action.gopage;

						// Render in a target page
						if (goToPage) {
							// Bind a listener on the 'pagebeforeshow' event in order
							// to render bindings only after the page is shown
							$(document).one('pagebeforeshow', function (event){
								C8O.renderBindings(c8oRequestable, $doc);
								if (action.afterRendering) {
									action.afterRendering($doc);
								}
							});
							
							// Change page
							$.mobile.changePage(goToPage, transition);
						}
						// Render on the same page
						else {
							C8O.renderBindings(c8oRequestable, $doc);
							if (action.afterRendering) {
								action.afterRendering($doc);
							}
						}
						
						return;
					}
				}
			}
		}
	},

	/**
	 * Scans all the widgets requiring data update and render them
	 * 
	 * If widgets are listviews, refresh them as well as iscroll widgets.
	 */
	renderBindings: function(calledRequestable, $xmlData) {
		$("[data-c8o-listen]").each( function (index, element) {
			var $element = $(element);
			
			// Get the binded attributes
			var listenRequestables = $element.attr('data-c8o-listen');
			
			// Check called requestable against the list of listen requestables
			if (C8O.isMatching(listenRequestables, calledRequestable)) {
				// Check listen condition if any
				var listenCondition = $element.attr("data-c8O-listen-condition");
				if (listenCondition) {
					var condition = $xmlData.find("*").andSelf().filter(listenCondition);
					if (condition.length == 0) {
						// The condition failed, so we abort the rendering
						return;
					}
				}

				// Apply the template
				var $c8oListenContainer = $(this);
				
				C8O.manageTemplate($c8oListenContainer);

				C8O.renderWidgets($c8oListenContainer, $xmlData);

				// Re attach any click handlers in case of new graphical components
				C8O.attachEventHandlers();
				
				// Check all the iscroll divs and refresh them to resize the iscroll size. 
				$("[data-iscroll]").each( function (index, element) {
					var $element = $(element);
					if (C8O.isDefined($element.iscrollview)) {
						$element.iscrollview("refresh");
					}
				});
			}
		});
	},
	
	isMatching: function(listenRequestables, calledRequestable) {
		var arrayListenRequestables = listenRequestables.split(",");
		
		// TODO: handle project.* or project.connector.*
		return (
				(listenRequestables === "*")
				|| (listenRequestables === calledRequestable)
				|| ($.inArray(calledRequestable, arrayListenRequestables) != -1)
			);
	},

	templates : {},
	
	addTemplate: function(template) {
		var templateID = Math.floor(Math.random() * 16777215).toString(16);
		C8O.templates[templateID] = template;
		return templateID;		
	},
	
	getTemplate: function(templateID) {
		return C8O.templates[templateID].clone();
	},
	
	removeTemplate: function(templateID) {
		return delete C8O.templates[templateID];
	},
	
	manageTemplate: function($element) {
		// We should first save the widget template for future reuse. If the widget has already
		// been rendered, there is a special attribute 'data-c8o-template-id', whose value is the
		// name of the saved template.
		var templateID = $element.attr("data-c8o-template-id");
		if (templateID) {
			// The widget has already been rendered. We must empty it and reinsert the template.
			$element.empty();
			var $template = C8O.getTemplate(templateID);

			// If we are in an iterator template, we must return the template
			var c8oEachIterator = $element.attr("data-c8o-each");
			if (c8oEachIterator) {
				return $template;
			}

			// In other case, just append the new clean template
			$element.append($template);
		}
		else {
			// The widget has not yet been rendered: generate an unique template ID and save the
			// template.
			var $template = $element.children().clone();
			templateID = C8O.addTemplate($template);
			$element.attr("data-c8o-template-id", templateID);
			
			// If we are in an iterator template, we must remove the template
			var c8oEachIterator = $element.attr("data-c8o-each");
			if (c8oEachIterator) {
				$element.children().detach();
			}
			
			// We must return a cloned instance of the template, otherwise we will
			// modify the template stored in the template repository.
			return $template.clone();
		}
	},
	
	/**
	 * Render the specified widget using the templating engine
	 * 
	 * $html points to the widget dom
	 * $doc is the data to render in the widget
	 */
	renderWidgets: function ($html, $doc) {
		// Render simple elements
		C8O.renderElement($html, $doc);

		// Render "use" attributes
		C8O.renderUseAttributes($html);

		// Render GUI components
		//$html.find("[data-role='collapsible-set']").collapsibleset();
		//$html.find("[data-role='listview']").listview();
		$html.trigger("create");
	},
	
	// Attributes automatic replacement, with the following format:
	// data-c8o-use="<attribute name>:<attribute value>"
	// Useful for attributes such as src in IMG tags, or data-role in JQueryUI components
	/**
	 * special case for "src" attributes.
	 * 
	 * To avoid browser's get before the src attribute has been replaced, use instead the
	 * "data-c8o-use" attribute. The templating engine will create dynamically "src" attributes
	 * with data from data-src attribute.
	 */
	renderUseAttributes: function($html) {
		var c8oUse = $html.attr("[data-c8o-use]");
		C8O.renderUseAttribute($html, c8oUse);

		var $c8oUses = $html.find("[data-c8o-use]").each(function () {
			var $this = $(this);
			var c8oUse = $this.attr("data-c8o-use");
			C8O.renderUseAttribute($this, c8oUse);
		});
	},
	
	renderUseAttribute: function($html, c8oUse) {
		if (c8oUse) {
			var i = c8oUse.indexOf(":");
			if (i != -1) {
				var useAttribute = c8oUse.substring(0, i);
				var useAttributeValue = c8oUse.substring(i+1);
				if (useAttribute) {
					$html.attr(useAttribute, useAttributeValue);
					$html.removeAttr("data-c8o-use");
				}
			}
		}
	},
	
	renderElement: function($element, $doc) {
		// Render iterated elements
		$element.find("*").andSelf().filter("[data-c8o-each]").each(function() {
//		$($element.get().reverse()).find("*").andSelf().filter("[data-c8o-each]").each(function() {
			var $c8oEachContainer = $(this);

			var $template = C8O.manageTemplate($c8oEachContainer);

			// Now we can iterate over the XML data
			var c8oEachIterator = $c8oEachContainer.attr("data-c8o-each");
			$doc.find(c8oEachIterator).each(function () {
				var $item = $template.clone();
				C8O.renderElement($item, $(this));
				$c8oEachContainer.append($item);
			});
		});

		// Render simple elements
		C8O.walk($element, $doc,
			// C8O.renderText
			function (txt, $data) {
				var m = txt.match(C8O.re.selector);
				while (m) {
					if (m[1] === ".") {
						var data = $data.text();
						txt = txt.replace("{.}", data);
						m = txt.match(C8O.re.selector);
					}
					else {
						var $m = $data.find(m[1]);
						if ($m.length > 0) {
							var data = m[2] ? $m.attr(m[2]) : $m.text();
							txt = txt.replace("{" + m[1] + "}", data);
							m = txt.match(C8O.re.selector);
						}
						else 
							m = false;
					}
				}
				return txt;
			}
		);
	},
	
	/**
	 * Walk each node and attribute and call the specified function
	 */
	walk: function (elt, data, fn) {
		if (elt.nodeType) {
			if (elt.nodeType == Node.ELEMENT_NODE) {
				for (var i = 0; i < elt.attributes.length; i++) {
					var fnr = fn(elt.attributes[i].nodeValue, data);
					if (fnr != null) {
						elt.attributes[i].nodeValue = fnr;
					}
				}
				for (var i = 0; i < elt.childNodes.length; i++) {
					C8O.walk(elt.childNodes[i], data, fn);
				}
			} else if (elt.nodeType == Node.TEXT_NODE) {
				var fnr = fn(elt.nodeValue, data);
				if (fnr != null) {
					elt.nodeValue = fnr;
				}
			}
		} else if (elt.each) {
			elt.each(function () {
				C8O.walk(this, data, fn);
			});
		}
	},
	
	renderText: function (txt, $data) {
		var find = txt.search(C8O.re.accolade);
		var res = "";
		while (find != -1) {
			res += txt.substring(0, find);
			txt = txt.substring(find);
			var match = txt.match(C8O.re.accolade)[0];
			var rule = {};
			try {
				jmatch = match.replace(C8O.re.replaceSimpleQuote, "$1\"").replace(C8O.re.removeBackslash, "$1");
				rule = $.parseJSON(jmatch);
			} catch (e) {
				if (match.length > 0) {
					rule = {find : match.substring(1, match.length - 1)}
				}
			}
			
			var value = undefined;
			
			try {
				if (C8O.isDefined(rule.find)) {
					var $elt = $data.find(rule.find);
					if ($elt.length) {
						if (C8O.isDefined(rule.attr)) {
							value = $elt.attr(rule.attr);
						} else {
							value = $elt.text();
						}
					}
				}
			} catch (e) {
				console.log(e);
			}
			
			if (C8O.isUndefined(value)) {
				if (C8O.isDefined(rule.def)) {
					value = rule.def;
				} else {
					value = "";
				}
			}
			
			res += value;
			txt = txt.substring(match.length);
			find = txt.search(C8O.re.accolade);
		}
		return res + txt;
	},
	
	decodeStore: function(store) {
		// Requestable format:
		// [<project name>].(([connector name].[<transaction name>]) | <sequence name>)[:<store name>]
		var storeParts = store.split(":");
		var requestable = storeParts[0];
		var storeID = null;
		if (storeParts.length == 2) {
			storeID = storeParts[1];
		}
		
		var requestableParts = requestable.split(".");
		
		switch (requestableParts.length) {
		// Transaction case: project.connector.transaction
		// project, connector and transaction are optional: if not set, we will use the current project, the default connector or the default transaction
		// E.g.:
		//    myproject.myconnector.mytransaction defines the transaction named "mytransaction" from the connector named "myconnector" from the project named "myproject"
		//    myproject..mytransaction defines the transaction named "mytransaction" from the default connector from the project named "myproject"
		//    .myconnector.mytransaction defines the transaction named "mytransaction" from the connector named "myconnector" from the current project
		//    ..mytransaction defines the transaction named "mytransaction" from the default connector from the current project
		//    .. defines the default transaction from the default connector from the current project
		case 3:
			return { storeID: storeID, project: requestableParts[0], connector: requestableParts[1], transaction: requestableParts[2] };
		// Sequence case: project.sequence
		// project is optional: if not set, we will use the current project
		case 2:
			return { storeID: storeID, project: requestableParts[0], sequence: requestableParts[1] };
		default:
			return {};
		}
	},
	
	/**
	 * Todo : Save in local storage any input field having the
	 * data-c8O-autosave attriute
	 */
	handleAutoSaveInputs: function(target) {
		
	},
	
	attachEventHandlers: function() {
		$("[data-c8o-call]").each( function (index, element) {
			var $element = $(element);
			
			$element.off('click.c8o').on('click.c8o', function() {
				var c8oCall = $element.attr("data-c8O-call");
				if (c8oCall) {
					var store = C8O.decodeStore(c8oCall);
					
					var c8oCallParams = {};
					
					if (store.project)
						c8oCallParams["__project"] = store.project;
					if (store.connector)
						c8oCallParams["__connector"] = store.connector;
					if (store.transaction)
						c8oCallParams["__transaction"] = store.transaction;
					if (store.sequence)
						c8oCallParams["__sequence"] = store.sequence;

					var context = $element.attr("data-c8O-context");
					if (context) {
						c8oCallParams["__context"] = context;
					}

					var c8oUserReference = $element.attr("data-c8O-user-reference");
					if (c8oUserReference) {
						c8oCallParams["__user_reference"] = c8oUserReference;
					}
					
					// Find whether the call compionent is inside a form
					var $form = $element.closest("form");
					if ($form.length > 0) {
						// Cancel form submissions as we are going to handle them by ourselves
						$form.submit(function () {
						    return false;
						});
						
						// Search for input fields in the form
						var formFields = C8O._parseQuery({}, $form.serialize());
						$.extend(c8oCallParams, formFields);						
					}
					else {
						// Search for 'data-c8o-variable' tagged elements in the link to use it
						// to build data request to C8O
						$element.find("[data-c8o-variable]").each(function (index, element) {
							var $c8oVariable = $(element);
							var name = $c8oVariable.attr("data-c8o-variable");
							var value = $c8oVariable.text();
							c8oCallParams[name] = value;
						});
					}

					// now call c8o with constructed arguments
					C8O.call(c8oCallParams);
				}
			});
		});
	}
};



$.ajaxSettings.traditional = true;
$.ajaxSetup({
	type : C8O.vars.ajax_method,
	dataType : "xml"
});

$(document).ready(function () {
	if (!$.mobile.ajaxBlacklist) {
		$("<div id=\"c8oloading\"/>").css({backgroundColor : "grey", position : "fixed", width : "100%", height : "100%", opacity : 0.5, "z-index" : 99}).hide().appendTo("body");
	}
	C8O._init();
	//C8O._cleanCache();
	C8O._hook("document_ready");
});

/**
 *  Initialize C8O MVC Framework 
 */
C8O.addHook("document_ready", function () {
	$(document).on("pagebeforecreate", "[data-role = page]", function(event){
		C8O.attachEventHandlers();
		
		var $document = $(document);

		// Store listen and iteration templates
		// Templates should be managed deeply first (in order to handle nested templates)
		// The search order is reversed because we want to manage templates deeply first.
		// This is not strictly deeply traversal, but it is enough to have deeply first
		// template management on each XML branch.
		$($document.find("[data-c8o-listen],[data-c8o-each]").get().reverse()).each(function() {
			var $c8oListenContainer = $(this);
			C8O.manageTemplate($c8oListenContainer);
		});
	});
	
});

/**
 * Hook the XML response for C8O MVC framework
 */
C8O.addHook("xml_response", function (xml, c8oCall) {
	/*
	 * Retrieving last requestable from last call to Convertigo server.
	 * This will help determining which actions have to be achieved: which data to manage in which screen.
	 * The choice can be done depending on the last "requestable", but also on the xml response itself, depending
	 * on its content (see above code).
	 */
	var c8oRequestable = project + "." + sequence;

	var project = c8oCall.__project;
	if (C8O.isUndefined(project)) project = "";

	var sequence = c8oCall.__sequence;
	if (C8O.isUndefined(sequence)) {
		var connector = c8oCall.__connector;
		if (C8O.isUndefined(connector)) connector = "";
		var transaction = c8oCall.__transaction;
		if (C8O.isUndefined(transaction)) transaction = "";
		c8oRequestable = project + "." + connector + "." + transaction;
	}
	else {
		c8oRequestable = project + "." + sequence;
	}
	
	C8O.routeResponse(c8oRequestable, xml);
});