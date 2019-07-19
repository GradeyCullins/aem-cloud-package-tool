function cloudEnableDisableMainCheck(){
    var importBtn = $('.import-btn');
    if ($('.cloud-main-check input').is(':checked')) {
        importBtn.removeAttr('disabled');
    } else {
        importBtn.attr('disabled', true);
    }
}

function cloudEnableDisableButtons() {
    var importBtn = $('.import-btn');
    $(".cloud-coralCheck input").each(function () {
        if ($(this).is(':checked')) {
            importBtn.removeAttr('disabled');
            return false;
        }
        importBtn.attr('disabled', true);
    });
}

function refreshUserPackages() {
    $(".cloud-loading-frame").css({'display':'flex'});
    //Get applied filters
    var nameFilter = $('.name-user-filter').val();
    var querystring = nameFilter.length == 0 ? '' : '?user-name=' + nameFilter;

    $.ajax({
        url:"/apps/hoodoo-digital/aem-cloud/package-tool/content/package-tool/jcr:content/content/items/columns/items/cloud/items/well/items/tabs/items/user/items/packagetool.html" + querystring,
        success: function(result) {
            $("#user-table-body").replaceWith($(result).find('#user-table-body'));
            showUserPackageDate();
            $(".cloud-loading-frame").css({'display':'none'});
        }
    });
}

function refreshBuildPackages() {
    $(".cloud-loading-frame").css({'display':'flex'});
    //Get applied filters
    var repositoryFilter = $('.repository-build-filter').val();
    var branchFilter = $('.branch-build-filter').val();
    var buildIdFilter = $('.buildid-build-filter').val();
    var nameFilter = $('.name-build-filter').val();
    var querystring = '?build-repository=' + repositoryFilter;
    querystring = branchFilter.length == 0 ? querystring : querystring + '&build-branch=' + branchFilter;
    querystring = buildIdFilter.length == 0 ? querystring : querystring + '&build-buildid=' + buildIdFilter;
    querystring = nameFilter.length == 0 ? querystring : querystring + '&build-name=' + nameFilter;
    $.ajax({
        url:"/apps/hoodoo-digital/aem-cloud/package-tool/content/package-tool/jcr:content/content/items/columns/items/cloud/items/well/items/tabs/items/cloud/items/packagetool.html" + querystring,
        success: function(result) {
            $("#build-table-body").replaceWith($(result).find('#build-table-body'));
            showBuildPackageDate();
            $(".cloud-loading-frame").css({'display':'none'});
        }
    });
}

function importPackagesDialog() {
    var dialog = document.querySelector('#import-dialog');
    dialog.show();
}

function importPackages() {
    var importItems = {
        items: []
    };
    var install = $("coral-checkbox[name='installOnImport']")[1].checked;
    var replicate = $("coral-checkbox[name='replicateOnImport']")[1].checked;

    $(".cloud-coralCheck input").each(function () {
        if ($(this).is(':checked')) {
            var itemId = $(this).attr('name');
            var item = {
                "itemId" : itemId,
                "install" : install,
                "replicate" : replicate
            };
            importItems.items.push(item);
        }
    })
    if (importItems.items.length > 0) {
        $.ajax({
            type:'POST',
            url: "/bin/aem-cloud/package-tool.import.json",
            data:{
                "importItems": JSON.stringify(importItems)
            },
            success: function(result){
                alert(result);
                refreshLocalPackages();
            }
        });
    }
}

