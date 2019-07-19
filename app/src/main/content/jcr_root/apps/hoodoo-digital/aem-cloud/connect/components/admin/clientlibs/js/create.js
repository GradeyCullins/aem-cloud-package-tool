(function (document, $) {

    "use strict";
    var COMMAND_URL= Granite.HTTP.externalize("/bin/wcmcommand");
    var contentPath = null;
    var relCreateConfig = ".create-config-aemcloud";
    var uniqueConfigNodeName = "aemcloudconnect";
    var createConfigNone = "none";
    var createConfigActivatorClass = "cq-confadmin-actions-createconfig-activator";
    var deleteConfigActivatorClass = ".cq-confadmin-actions-delete-activator";
    $(document).on("foundation-contentloaded", initCreateConfig);
    $(document).on("click", relCreateConfig, openCreate);
    $(document).on("click", deleteConfigActivatorClass, deleteConfigMessage);

    $(document).on("foundation-selections-change", function (e) {

        var createConfigButton = document.querySelector(relCreateConfig);
        createConfigButton.hide();
        var localName = e.target.localName;
        if(e.target.activeItem){
            var activeItem = e.target.activeItem;
            checkCreateConfig($(activeItem));
        }

    });
    function checkCreateConfig(activeItem){

        var activeItemMeta = activeItem.find('.foundation-collection-quickactions');
        if(activeItemMeta)
            var relAction = activeItemMeta.data('foundation-collection-quickactions-rel');
        var createConfigButton = document.querySelector(relCreateConfig);
        createConfigButton.hide();
        if(relAction && createConfigButton && relAction===createConfigNone)
            createConfigButton.hide();
        if(relAction && createConfigButton && relAction===createConfigActivatorClass)
            createConfigButton.show();




    }

    function initCreateConfig() {

        //Default content path
        var activeItem = $(".cq-confadmin-admin-childpages.foundation-collection");
        if(activeItem !=undefined){
            contentPath = activeItem.data("foundationCollectionId");
            activeItem.on("coral-columnview:loaditems", function () {
                var activeColumnItem = $(".cq-confadmin-admin-childpages.foundation-collection .is-active");
                if (activeColumnItem.length > 0)
                    activeColumnItem.each(function (i, objElement) {
                        if (contentPath === $(objElement).data('foundation-collection-item-id') || contentPath === $(objElement).data('foundation-collection-item-id')+ "/" )
                            checkCreateConfig($(objElement));
                    })

            });

        }

    }
    function openCreate() {
        //Default content path
        var activeItem = $(".cq-confadmin-admin-childpages.foundation-collection");
        if(activeItem !=undefined){
            contentPath = activeItem.data("foundationCollectionId");
            activeItem.on("coral-columnview:loaditems", function () {
                var activeColumnItem = $(".cq-confadmin-admin-childpages.foundation-collection .is-active");
                if (activeColumnItem.length > 0)
                    activeColumnItem.each(function (i, objElement) {
                        if (contentPath === $(objElement).data('foundation-collection-item-id') || contentPath === $(objElement).data('foundation-collection-item-id')+ "/" )
                            checkCreateConfig($(objElement));
                    })

            });
            location.href = Granite.HTTP.externalize("/apps/hoodoo-digital/aem-cloud/connect/content/configurations/createaemcloudconfig.html"+contentPath);
        }
    }

    // Deletion related methods and click handler
    var deleteText;
    function getDeleteText() {
        if (!deleteText) {
            deleteText = Granite.I18n.get("Delete");
        }
        return deleteText;
    }

    var cancelText;
    function getCancelText() {
        if (!cancelText) {
            cancelText = Granite.I18n.get("Cancel");
        }
        return cancelText;
    }

    function createEl(name) {
        return $(document.createElement(name));
    }

    function deleteConfigMessage(){
        var message = createEl("div");
        var intro = createEl("p").appendTo(message);
        var selections = $(".foundation-selections-item");
        if (selections.length === 1) {
            intro.text(Granite.I18n.get("You are going to delete the following item:"));
        } else {
            intro.text(Granite.I18n.get("You are going to delete the following {0} items:", selections.length));
        }
        var list = [];
        var maxCount = Math.min(selections.length, 12);
        for (var i = 0, ln = maxCount; i < ln; i++) {
            var title = $(selections[i]).find(".foundation-collection-item-title").text();
            list.push(createEl("b").text(title).prop("outerHTML"));
        }
        if (selections.length > maxCount) {
            list.push("&#8230;"); // &#8230; is ellipsis
        }

        createEl("p").html(list.join("<br>")).appendTo(message);

        var ui = $(window).adaptTo("foundation-ui");

        ui.prompt(getDeleteText(), message.html(), "notice", [{
            text: getCancelText()
        }, {
            text: getDeleteText(),
            warning: true,
            handler: function() {

                deleteConfig();
            }
        }]);
    }

    function deleteConfig() {
        var paths=[];
        var items = $(".foundation-selections-item");
        if (items.length) {

            items.each(function(i) {
                var item = $(this);
                var itemPath = item.data("foundation-collection-item-id");
                paths.push(itemPath);

            });

            $.ajax({
                url: COMMAND_URL,
                type: "POST",
                data: {
                    _charset_: "UTF-8",
                    cmd: "deletePage",
                    path: paths,
                    force: false,
                    checkChildren: true
                },
                success: function() {
                    location.reload();
                }

            })
        }

    }

})(document, Granite.$);
