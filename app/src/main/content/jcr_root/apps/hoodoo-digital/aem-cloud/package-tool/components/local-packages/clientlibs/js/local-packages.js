$(document).ready(function () {
    //Get querystring parameters
    $(".loading-frame").css({'display':'none'});
    $(".cloud-loading-frame").css({'display':'none'});
    var userNameFilter = getParameterByName('user-name');

    if (userNameFilter) {
        //Load with user packages filters open
        var accordion = $('#user-filters');
        accordion.prop("selected", true);

    } else {
        var buildRepositoryFilter = getParameterByName('build-repository');
        var buildBranchFilter = getParameterByName('build-branch');
        var buildBuildIdFilter = getParameterByName('build-buildid');
        var buildNameFilter = getParameterByName('build-name');

        if (buildRepositoryFilter || buildBranchFilter || buildBuildIdFilter || buildNameFilter) {
            //Load with build packages filters open
            var accordion = $('#build-filters');
            accordion.prop("selected", true);
        } else {
            var localGroupFilter = getParameterByName('local-group');
            var localNameFilter = getParameterByName('local-name');

            if (localGroupFilter || localNameFilter) {
                //Load with local packages filters open
                var accordion = $('#local-filters');
                accordion.prop("selected", true);
            }
        }
    }

    $(".schedule-button").click(function (event) {
        event.preventDefault();
        var dialog = document.querySelector('#schedule-dialog');
        dialog.show();
    });

    showUserPackageDate();
    showBuildPackageDate();
    showLocalPackageNextDate();
    showLocalPackageDate();
});

//Show dates with moment js
function showLocalPackageNextDate() {
    $('.localPackagesRow').each(function(index){
        var nextDate = parseInt($(".nextScheduleRun"+index).attr('nextDate'));
        $(".nextScheduleRun"+index).text("(next run " + moment(nextDate).fromNow() + ")");
    })
}

function showUserPackageDate() {
    $('.usercloudPackagesRow').each(function( index ) {
        var packageDate = parseInt($(".userpackageDate"+index).attr('packageDate'));
        $(".userpackageDate"+index).text(moment(packageDate).format('LLL'));
    });
}

function showBuildPackageDate() {
    $('.buildcloudPackagesRow').each(function( index ) {
        var packageDate = parseInt($(".buildpackageDate"+index).attr('packageDate'));
        $(".buildpackageDate"+index).text(moment(packageDate).format('LLL'));
    });
}

function showLocalPackageDate() {
    $('.localPackagesRow').each(function(index){
        var packageDate = parseInt($(".localPackageDate"+index).attr('packageDate'));
        $(".localPackageDate"+index).text(moment(packageDate).format('LLL'));
    });
}

function getParameterByName(name, url) {
    if (!url) url = window.location.href;
    name = name.replace(/[\[\]]/g, "\\$&");
    var regex = new RegExp("[?&]" + name + "(=([^&#]*)|&|#|$)"),
        results = regex.exec(url);
    if (!results) return null;
    if (!results[2]) return '';
    return decodeURIComponent(results[2].replace(/\+/g, " "));
}

function enableDisableMainCheck(){
    var scheduleBtn = $('.schedule-button');
    var sendToAemBtn = $('.send-to-aem');

    if ($('.main-check input').is(':checked')) {
        scheduleBtn.removeAttr('disabled');
        sendToAemBtn.removeAttr('disabled');
    } else {
        scheduleBtn.attr('disabled', true);
        sendToAemBtn.attr('disabled', true);
    }
}

function enableDisableButtons() {
    var scheduleBtn = $('.schedule-button');
    var sendToAemBtn = $('.send-to-aem');

    $(".coralCheck input").each(function () {
        if ($(this).is(':checked')) {
            scheduleBtn.removeAttr('disabled');
            sendToAemBtn.removeAttr('disabled');
            return false;
        }
        scheduleBtn.attr('disabled', true);
        sendToAemBtn.attr('disabled', true);
    });
}

function setSchedulePeriodDialog() {
    var scheduleItems = {
        items: []
    };
    var period = $('.coralSelectionDialog :selected').val();

    $(".coralCheck input").each(function () {
        if ($(this).is(':checked')) {
            var itemId = $(this).attr('name');
            var item = {
                "itemId" : itemId,
                "period" : period
            };
            scheduleItems.items.push(item);
        }
    })
    if (scheduleItems.items.length > 0) {
         $.ajax({
             type:'POST',
             url: "/bin/aem-cloud/package-tool.schedule.json",
             data:{
                 "scheduleItems": JSON.stringify(scheduleItems)
             },
             success: function(result){
                 alert(result);
                 refreshLocalPackages();
             }
         });
    }
}

function setSchedulePeriodDropdown(itemIndex) {
    var scheduleItems = {
        items: []
    };
    var item = {
        "itemId" : $('.localListCheckbox' + itemIndex).find('input').attr('name'),
        "period" : $('.localDropdown' + itemIndex).val()
    };
    scheduleItems.items.push(item);
    if (scheduleItems.items.length > 0) {
        $.ajax({
            type:'POST',
            url: "/bin/aem-cloud/package-tool.schedule.json",
            data:{
                "scheduleItems": JSON.stringify(scheduleItems)
            },
            success: function(result){
                alert(result);
                refreshLocalPackages();
            }
        });
    }
}

function refreshLocalPackages() {
    $(".loading-frame").css({'display':'flex'});

    //Get applied filters
    var groupFilter = $('.groupSelectionFilter').find("coral-select-item[selected]");
    var nameFilter = $('.nameSelectionFilter').val();
    var scheduleFilter = $('.scheduleSelectionFilter').val();
    var querystring = '?local-group=';
    if (groupFilter.length >= 1) {
        groupFilter.each( function(i,item) {
            querystring = querystring + $(item).val() + ',';
        })
    } else {
        querystring = querystring + 'all';
    }
    querystring = nameFilter.length == 0 ? querystring + '&local-schedule=' + scheduleFilter : querystring + '&local-name=' + nameFilter + '&local-schedule=' + scheduleFilter;

    $.ajax({
        url:"/apps/hoodoo-digital/aem-cloud/package-tool/content/package-tool/jcr:content/content/items/columns/items/local/items/well/items/tabs/items/local/items/packagetool.html" + querystring,
        success: function(result) {
            setTimeout( function() {
                $("#local-table-body").replaceWith($(result).find('#local-table-body'));
                showLocalPackageNextDate();
                showLocalPackageDate();
                $(".loading-frame").css({'display':'none'});
            }, 1000);
        }
    });
}

function submitIfEnterIsPressed(event) {
    if (event.keyCode == 13 || event.which == 13){
        refreshLocalPackages();
    }
}

function sendPackages() {
    var sendItems = {
        items: []
    };

    $(".coralCheck input").each(function () {
        if ($(this).is(':checked')) {
            var itemId = $(this).attr('name');
            var item = {
                "itemId" : itemId
            };
            sendItems.items.push(item);
        }
    })
    if (sendItems.items.length > 0) {
        $.ajax({
            type:'POST',
            url: "/bin/aem-cloud/package-tool.send.json",
            data:{
                "sendItems": JSON.stringify(sendItems)
            },
            success: function(result){
                alert(result);
                refreshUserPackages();
                refreshBuildPackages();
            }
        });
    }
}
