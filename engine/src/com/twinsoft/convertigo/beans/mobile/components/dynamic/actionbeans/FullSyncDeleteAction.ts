    /**
     * Function FullSyncDeleteAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    FullSyncDeleteAction(page: C8oPageBase, props, vars) : Promise<any> {
        return new Promise((resolve, reject) => {
            let r:string = props.requestable.substring(props.requestable.indexOf('.') + 1);
            let v:string = 'delete';
            let m:string = props.marker;
            let rvm:string = r + '.' + v + ((m != '' && m!= null) ? '#' + m : '');
            let data = {};
            
            data['docid'] = props['docid'];
            if(data['rev'] != null){
                data['rev'] = props['rev'];
            }
            
            C8oCafUtils.merge(data, vars);
            
            page.getInstance(Platform).ready().then(() => {     // We may need the CBL plugin so wait for platform ready.
                page.c8o.finalizeInit().then(()=>{              // To be sure that FullSync initialized properly on CBL
                    page.call("fs://" + rvm, data, null, 500)
                    .then((res:any) => {resolve(res)}).catch((error:any) => {reject(error)})
                })
            });
        });
    }
