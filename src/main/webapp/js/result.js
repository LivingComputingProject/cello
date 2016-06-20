if (!isRegistered()) {
    window.location.replace('index.html');
}

var res = {}; //data model for results page
var data_true = [['Number of Modified Gates', 'Score Improvement']];
var data_false = [['Number of Modified Gates', 'Score Improvement']];
var data_metrics = [['score', 'correctness', 'snr', 'dsnr']];
var configuration = 'Not Defined';

$(document).ready(function () {

    if (sessionStorage.results_page) {
        var results_page = JSON.parse(sessionStorage.results_page);
        res.jobID = sessionStorage.jobID;
        res.show_nA = results_page.show_nA;
        res.result_tab = results_page.result_tab;
        res.files = results_page.files;
        res.all_result_files = results_page.all_result_files;
    } else {
        res.jobID = sessionStorage.jobID;
        res.show_nA = "000";
    }

    setResults();
}
);

function setResults() {
    set_result_pulldown();
}

if (typeof String.prototype.startsWith != 'function') {
    // see below for better implementation!
    String.prototype.startsWith = function (str) {
        return this.indexOf(str) === 0;
    };
}
if (typeof String.prototype.endsWith != 'function') {
    String.prototype.endsWith = function (str) {
        return this.slice(-str.length) == str;
    };
}

function set_filepaths() {

    var all_result_files = res.all_result_files;

    var prefix = res.jobID + "_";

    var files_obj = {};
    files_obj['verilog'] = prefix + "verilog.v";
    files_obj['dnac_log'] = prefix + "dnacompiler_output.txt";
    files_obj['localopt'] = prefix + "localoptimizer_output.txt";
    files_obj['metrics'] = prefix + "metricscalculation_output.txt";
    files_obj['nA_list'] = res.nA_list;

    for (var i = 0; i < res.nA_list.length; ++i) {
        var nA = res.nA_list[i];

        //var pad = "000";
        //var n = $('#show_nA').val();
        //var nA = "A" + (pad + n).slice(-pad.length);

        var nA_files = {};
        nA_files.output_reus = [];
        nA_files.plasmids = [];
        nA_files.sbol = [];

        for (var j = 0; j < all_result_files.length; ++j) {
            var file = all_result_files[j];

            if (file.indexOf(prefix + "A" + nA) != -1) {

                //single files
                if (file.endsWith("_wiring_grn.png")) {
                    nA_files.wiring_grn = file;
                }
                if (file.endsWith("_wiring_reu.png")) {
                    nA_files.wiring_reu = file;
                }
                if (file.endsWith("_wiring_xfer.png")) {
                    nA_files.wiring_xfer = file;
                }
                if (file.endsWith("_reutable.txt")) {
                    nA_files.reutable = file;
                }
                if (file.endsWith("_toxtable.txt")) {
                    nA_files.toxtable = file;
                }
                if (file.endsWith("_logic_circuit.txt")) {
                    nA_files.logic_circuit = file;
                }
                if (file.endsWith("_bionetlist.txt")) {
                    nA_files.bionetlist = file;
                }
                if (file.endsWith("_circuit_module_rules.eug")) {
                    nA_files.eugene = file;
                }
                if (file.endsWith("_dnaplotlib_Eu_out.png")) {
                    nA_files.dnaplotlib = file;
                }

                //list of files
                if (file.endsWith("_truth.png")) {
                    nA_files.output_reus.push(file);
                }
                if (file.endsWith(".ape")) {
                    nA_files.plasmids.push(file);
                }
                if (file.endsWith(".sbol")) {
                    nA_files.sbol.push(file);
                }
            }
        }

        files_obj[nA] = nA_files;
    }

    res.files = files_obj;

    showResults();

}



$("#result_pulldown").change(function () {
    res.jobID = $('#result_pulldown').val();
    set_nA_list();
});

$("#show_nA").change(function () {
    res.show_nA = $('#show_nA').val();
    set_filepaths();
});


$("#download_zip").on("click", function () {
    downloadZip();
});


function set_all_result_files() {
    $.ajax({
        url: "results/" + res.jobID,
        type: "GET",
        headers: {
            "Authorization": "Basic " + btoa(sessionStorage.username + ":" + sessionStorage.password)
        },
        data: {
            keyword: "",
            extension: ""
        },
        dataType: "json",
        success: function (response) {
            var filenames = response['files'];
            res.all_result_files = filenames;
            set_nA_list();
            return true;
        },
        error: function () {
            return true;
        }
    });
}


function set_nA_list() {

    var nA_list = [];

    for (var i = 0; i < res.all_result_files.length; ++i) {
        var filename = res.all_result_files[i];
        if (filename.indexOf("_logic_circuit") > -1) {
            var l = filename.split('_logic_circuit')[0].length;
            var nA = filename.split('_logic_circuit')[0].substring(l - 3, l);
            nA_list.push(nA);
        }
    }

    res.nA_list = nA_list;

    set_nA_pulldown();
}


function set_nA_pulldown() {

    var x = document.getElementById("show_nA");
    removeOptions(x);

    for (var i = 0; i < res.nA_list.length; ++i) {
        var custom = document.createElement("option");
        var nA = res.nA_list[i];
        custom.text = nA;
        custom.value = nA;
        x.add(custom);

        var exists = false;
        $('#show_nA option').each(function () {
            if (this.value === res.show_nA) {
                exists = true;
                return false;
            }
        });

        if (!exists) {
            $("#show_nA option:first");
            res.show_nA = $('#show_nA').val();
        } else {
            $('#show_nA').val(res.show_nA);
        }
    }

    set_filepaths();
}


function set_result_pulldown() {

    var x = document.getElementById("result_pulldown");

    $.ajax({
        url: "results",
        type: "GET",
        headers: {
            "Authorization": "Basic " + btoa(sessionStorage.username + ":" + sessionStorage.password)
        },
        data: {
        },
        dataType: "json",
        success: function (response) {
            var dirnames = response['folders'];

            for (var i = 0; i < dirnames.length; ++i) {
                if (dirnames[i] != '') {
                    var custom = document.createElement("option");
                    custom.text = dirnames[i].replace(/(\r\n|\n|\r)/gm, "");
                    custom.value = dirnames[i].replace(/(\r\n|\n|\r)/gm, "");
                    x.add(custom);
                }
            }

            var exists = false;
            $('#result_pulldown option').each(function () {
                if (this.value == res.jobID) {
                    exists = true;
                    return false;
                }
            });

            if (!exists) {
                $("#result_pulldown option:first");
                res.jobID = $('#result_pulldown').val();
            } else {
                $('#result_pulldown').val(res.jobID);
            }

            set_all_result_files();
        },
        error: function () {
            return true;
        }
    });
}


$("#delete_result").click(function () {

    if ($('#result_pulldown').val() == null) {
        return false;
    }

    if (confirm("delete result " + $('#result_pulldown').val() + "?")) {
        var jobID = $('#result_pulldown').val()
        $.ajax({
            url: "results/" + jobID,
            type: "DELETE",
            headers: {
                "Authorization": "Basic " + btoa(sessionStorage.username + ":" + sessionStorage.password)
            },
            data: {
            },
            dataType: "json",
            success: function (response) {
                location.reload();
            }
        });
    }
});


function removeOptions(selectbox)
{
    var i;
    for (i = selectbox.options.length - 1; i >= 0; i--)
    {
        selectbox.remove(i);
    }
}


$("#r1").mousedown(function () {
    show(1);
    res.result_tab = 1;
});
$("#r2").mousedown(function () {
    show(2);
    res.result_tab = 2;
});
$("#r3").mousedown(function () {
    show(3);
    res.result_tab = 3;
});
$("#r4").mousedown(function () {
    show(4);
    res.result_tab = 4;
});
$("#r5").mousedown(function () {
    show(5);
    res.result_tab = 5;
});
$("#r6").mousedown(function () {
    show(6);
    res.result_tab = 6;
});
$("#r7").mousedown(function () {
    show(7);
    res.result_tab = 7;
});
$("#r8").mousedown(function () {
    show(8);
    res.result_tab = 8;
});


function show(x) { //hide all, then display current mousedown block
    $('#view1').hide();
    $('#view2').hide();
    $('#view3').hide();
    $('#view4').hide();
    $('#view5').hide();
    $('#view6').hide();
    $('#view7').hide();
    $('#view8').hide();
    $('#view' + x).show();
}
function dontshow(x) {
    $(x).hide();
}

function downloadZip() {

    if ($('#result_pulldown').val() == null) {
        return false;
    }

    var jobID = $('#result_pulldown').val();
    $.ajax({
        url: "downloadzip/" + jobID,
        type: "GET",
        headers: {
            "Authorization": "Basic " + btoa(sessionStorage.username + ":" + sessionStorage.password)
        },
        data: {
        },
        success: function (response) {
            var uriContent = "data:application/zip;base64," + response;
            var encodedUri = uriContent;

            var pom = document.createElement('a');
            pom.setAttribute('href', encodedUri);
            pom.setAttribute('download', $('#result_pulldown').val() + ".zip");
            pom.style.display = 'none';
            document.body.appendChild(pom);
            pom.click();
            document.body.removeChild(pom);
        },
        error: function () {
        }
    });
}


function showFile(filename, div_id, element_id) {

    if (filename != null && filename != undefined && filename.length > 0) {

        if (filename.indexOf("png") > -1) {
            ajaxPNG(filename, div_id, element_id);
        } else {
            ajaxTXT(filename, div_id, element_id);
        }

    }
}

//Optimize

function showChart() {
    var options = {
        title: 'Protein Engineering',
        chartArea: {width: '100%'},
        hAxis: {
            title: 'Score Improvement  Percentage',
            minValue: 0
        },
        vAxis: {
            title: 'Number of Modified Gates'
        }
    };

    function selectHandlerTrue() {
        var selectedItem = chart_true.getSelection()[0];
        if (selectedItem) {
            var topping = protein_data.getValue(selectedItem.row, 0);
            ajaxDES(true, topping - 1);
            alert('Configuration: ' + configuration);
        }
    }


    var protein_data = google.visualization.arrayToDataTable(data_true);
    var chart_true = new google.visualization.BarChart(document.getElementById('protein_engineering'));
    google.visualization.events.addListener(chart_true, 'select', selectHandlerTrue);
    chart_true.draw(protein_data, options);

    options = {
        title: 'DNA Engineering',
        chartArea: {width: '100%'},
        hAxis: {
            title: 'Score Improvement Percentage',
            minValue: 0
        },
        vAxis: {
            title: 'Number of Modified Gates'
        }
    };

    function selectHandlerFalse() {
        var selectedItem = chart_false.getSelection()[0];
        if (selectedItem) {
            var topping = dna_data.getValue(selectedItem.row, 0);
            ajaxDES(true, topping - 1);
            alert('Configuration: ' + configuration);
        }
    }


    var dna_data = google.visualization.arrayToDataTable(data_false);
    var chart_false = new google.visualization.BarChart(document.getElementById('dna_engineering'));
    google.visualization.events.addListener(chart_false, 'select', selectHandlerFalse);
    chart_false.draw(dna_data, options);
}


// Metrics
function showMetrics() {
    var radiusScore = [];
    var radiusSNR = [];
    var radiusDSNR = [];
    var angle = [];
    var x = [];
    for (var i = 1; i < data_metrics.length; i++) {
        radiusScore = radiusScore.concat([data_metrics[i][0]]);
        radiusSNR = radiusSNR.concat([data_metrics[i][2]]);
        radiusDSNR = radiusDSNR.concat([data_metrics[i][3]]);
        angle = angle.concat([data_metrics[i][1]]);
        x = x.concat([i]);
    }
    var score = {
        r: radiusScore,
        t: angle,
        mode: 'markers',
        name: 'Score',
        marker: {
            color: 'rgb(230,171,2)',
            size: 60,
            line: {color: 'white'},
            opacity: 0.7
        },
        type: 'scatter'
    };

    var snr = {
        r: radiusSNR,
        t: angle,
        mode: 'markers',
        name: 'SNR',
        marker: {
            color: 'rgb(100,1,200)',
            size: 60,
            line: {color: 'white'},
            opacity: 0.7
        },
        type: 'scatter'
    };

    var dsnr = {
        r: radiusDSNR,
        t: angle,
        mode: 'markers',
        name: 'DSNR',
        marker: {
            color: 'rgb(30,171,100)',
            size: 60,
            line: {color: 'white'},
            opacity: 0.7
        },
        type: 'scatter'
    };
    
    var scoreStat = {
        y: radiusScore,
        x: x,
        mode: 'lines+markers',
        name: 'Score',
        marker: {
            color: 'rgb(230,171,2)',
            size: 6,
            line: {color: 'white'},
            opacity: 0.7
        },
        type: 'scatter'
    };

    var snrStat = {
        y: radiusSNR,
        x: x,
        mode: 'lines+markers',
        name: 'SNR',
        marker: {
            color: 'rgb(100,1,200)',
            size: 6,
            line: {color: 'white'},
            opacity: 0.7
        },
        type: 'scatter'
        //, yaxis: 'y2'
    };

    var dsnrStat = {
        y: radiusDSNR,
        x: x,
        mode: 'lines+markers',
        name: 'DSNR',
        marker: {
            color: 'rgb(30,171,100)',
            size: 6,
            line: {color: 'white'},
            opacity: 0.7
        },
        type: 'scatter'
        //, yaxis: 'y3'
    };
    
    var correctnessStat = {
        y: angle,
        x: x,
        mode: 'lines+markers',
        name: 'Correctness',
        marker: {
            color: 'rgb(30,10,100)',
            size: 6,
            line: {color: 'white'},
            opacity: 0.7
        },
        type: 'scatter'
        //, yaxis: 'y4'
    };

    var traceDummy = {
        r: [],
        t: [],
        mode: 'markers',
        name: 'Trial 6',
        marker: {
            color: 'rgb(230,171,2)',
            size: 110,
            line: {color: 'white'},
            opacity: 0.7
        },
        type: 'scatter',
        connectgaps: true,
        showlegend: false
    };


    var data = [score, snr, dsnr];
    var dataStat = [scoreStat, snrStat, dsnrStat, correctnessStat];
    var dataScore = [score, traceDummy, traceDummy];
    var dataSNR = [snr, traceDummy, traceDummy];
    var dataDSNR = [dsnr, traceDummy, traceDummy];

    var layout = {
        radialaxis: {
            //range: [40, 42]
        },
        title: 'Various Metrics vs. Correctness',
        font: {size: 15},
        plot_bgcolor: 'rgb(223, 223, 223)',
        paper_bgcolor: 'rgb(245, 245, 245)',
        angularaxis: {tickcolor: 'rgb(253,253,253)',
            range: [0, 360]
        },
        direction: 'counterclockwise'
    };
    
    var layoutStat = {
        title: 'Various Metrics vs. Assignment',
        font: {size: 15},
        plot_bgcolor: 'rgb(223, 223, 223)',
        paper_bgcolor: 'rgb(245, 245, 245)'/*,
        yaxis: {
    title: 'Score',
    titlefont: {color: 'rgb(230,171,2)'},
    tickfont: {color: 'rgb(230,171,2)'}
  },
  yaxis2: {
    title: 'SNR',
    titlefont: {color: 'rgb(100,1,200)'},
    tickfont: {color: 'rgb(100,1,200)'},
    overlaying: 'y',
    side: 'right'
  },
  yaxis3: {
    title: 'DSNR',
    titlefont: {color: 'rgb(30,171,100)'},
    tickfont: {color: 'rgb(30,171,100)'},
    overlaying: 'y',
    side: 'right'
  },
  yaxis4: {
    title: 'Correctness',
    titlefont: {color: 'rgb(30,10,100)'},
    tickfont: {color: 'rgb(30,10,100)'},
    overlaying: 'y',
    side: 'left',
    position: -0.15,
    anchor: 'free'
  }*/
    };


    Plotly.newPlot('correctness', data, layout);
    Plotly.newPlot('statistics', dataStat, layoutStat);
    //Plotly.newPlot('correctnessScore', dataScore, layout);
    //Plotly.newPlot('correctnessSNR', dataSNR, layout);
    //Plotly.newPlot('correctnessDSNR', dataDSNR, layout);
}





function showResults() {

    var nA_files = res.files[res.show_nA];

    //single files
    showFile(res.files.verilog, "div1a", "file1a");
    showFile(res.files.dnac_log, "div1b", "file1b");
    showFile(nA_files.wiring_grn, "div2a", "file2a");
    showFile(nA_files.wiring_xfer, "div2c", "file2c");
    showFile(nA_files.reutable, "div2d", "file2d");
    showFile(nA_files.toxtable, "div2e", "file2e");
    showFile(nA_files.logic_circuit, "div2f", "file2f");
    showFile(nA_files.bionetlist, "div2g", "file2g");
    showFile(nA_files.wiring_reu, "div3a", "file3a");
    showFile(nA_files.eugene, "div4a", "file4a");
    showFile(nA_files.dnaplotlib, "div4b", "file4b");

    //file lists
    showImgSet(nA_files.output_reus, "div3b", "img3set");
    showPlasmidFiles(nA_files.plasmids, 'plasmid_list');
    showSBOLFiles(nA_files.sbol, 'sbol_list');

    //Optimize
    //ajaxOPT();
    //showFile(res.files.localopt, "original_score", "file7");

    //Metrics
    ajaxMetrics();
    //showMetrics();

    if (res.result_tab > 0) {
        show(res.result_tab);
    } else {
        show(1);
    }

    return;
}

function showImgSet(filenames, div_id, element_id) {
    //output REU files
    $('#' + element_id).html("");

    for (var i = 0; i < filenames.length; ++i) {
        var filename = filenames[i].trim();
        if (filename.length > 0) {
            $('#' + element_id).append("" +
                    "<p style='text-align: left'>" + filename + "</p>" +
                    "<img id='outreu" + i + "' style='width:50%'>"
                    );
            ajaxPNG(filename, div_id, 'outreu' + i);
            $('#' + div_id).show();
        } else {
            $('#' + div_id).hide();
        }
    }
}

function showSBOLFiles(filenames, element_id) {
    var sbol_html = "";

    for (var i = 0; i < filenames.length; ++i) {
        if (filenames[i]) {
            filenames[i].trim();

            var tokens = filenames[i].split("/");
            var filename = tokens[tokens.length - 1];

            sbol_html += "<p class='sbol_link' onclick='showPlasmid(this)'" + ">" + filename + "</p>";
        } else {
        }
    }
    $('#' + element_id).html(sbol_html);

}


function showPlasmidFiles(filenames, element_id) {

    //ape files
    var plasmids_html = "";

    for (var i = 0; i < filenames.length; ++i) {
        if (filenames[i]) {
            filenames[i].trim();

            var tokens = filenames[i].split("/");
            var filename = tokens[tokens.length - 1];

            plasmids_html += "<p id=plasmid_link" + i + " class='plasmid_link' onclick='showPlasmid(this)'" + ">" + filename + "</p>";
            $('#div5a').show();
            $('#div5b').show();
        }

    }

    $('#' + element_id).html(plasmids_html);

    if (document.getElementsByClassName('plasmid_link').length > 0) {
        showPlasmid(document.getElementsByClassName('plasmid_link')[0]);
    }
}



function ajaxPNG(filename, div_id, element_id) {

    var jobID = res.jobID;
    $.ajax({
        url: "results/" + jobID + "/" + filename,
        type: "GET",
        headers: {
            "Authorization": "Basic " + btoa(sessionStorage.username + ":" + sessionStorage.password)
        },
        data: {
        },
        success: function (rawImageData) {
            $('#' + element_id).attr('src', "data:image/png;base64," + rawImageData);
            $('#' + div_id).show();
            return true;
        },
        error: function () {
            $('#' + div_id).hide();
            return true;
        }
    });
}

function ajaxTXT(filename, div_id, element_id) {

    var jobID = res.jobID;
    $.ajax({
        url: "results/" + jobID + "/" + filename,
        type: "GET",
        headers: {
            "Authorization": "Basic " + btoa(sessionStorage.username + ":" + sessionStorage.password)
        },
        data: {
        },
        success: function (response) {
            response = response.replace(/</g, '&lt;');
            $('#' + element_id).html(response);
            $('#' + div_id).show();
            return true;
        },
        error: function () {
            $('#' + div_id).hide();
            return true;
        }
    });
}

function ajaxOPT() {

    var filename = res.files.localopt;
    var jobID = res.jobID;
    $.ajax({
        url: "results/" + jobID + "/" + filename,
        type: "GET",
        headers: {
            "Authorization": "Basic " + btoa(sessionStorage.username + ":" + sessionStorage.password)
        },
        data: {
        },
        success: function (response) {
            response = response.replace(/</g, '&lt;');
            var obj = $.parseJSON(response);
            for (var i = 0; i < obj.scores_protein.length; i++) {
                data_true = data_true.concat([[i + 1, obj['scores_protein'][i]]]);
                data_false = data_false.concat([[i + 1, obj['scores_dna'][i]]]);
            }
            showChart();
            $('#original_score_value').html(obj['original_score']);
            $('#original_score').show();
            return true;
        },
        error: function () {
            $('#original_score_value').hide();
            return true;
        }
    });
}

function ajaxMetrics() {

    var filename = res.files.metrics;
    var jobID = res.jobID;
    $.ajax({
        url: "results/" + jobID + "/" + filename,
        type: "GET",
        headers: {
            "Authorization": "Basic " + btoa(sessionStorage.username + ":" + sessionStorage.password)
        },
        data: {
        },
        success: function (response) {
            response = response.replace(/</g, '&lt;');
            var obj = $.parseJSON(response);
            //alert(obj.score.length);
            data_metrics = [['score', 'correctness', 'snr', 'dsnr']];
            for (var i = 0; i < obj.score.length; i++) {
                //alert(obj['score'][i] + ',' + obj['correctness'][i] + ',' + obj['snr'][i] + ',' + obj['dsnr'][i]);
                data_metrics.push([obj['score'][i], obj['correctness'][i], obj['snr'][i], obj['dsnr'][i]]);
            }
            //alert(data_metrics.length);
            showMetrics();
            $('#score_value').html(data_metrics[1][0]);
            $('#score').show();
            $('#correctness_value').html(data_metrics[1][1]);
            $('#correctness').show();
            $('#snr_value').html(data_metrics[1][2]);
            $('#snr').show();
            $('#dsnr_value').html(data_metrics[1][3]);
            $('#dsnr').show();
            return true;
        },
        error: function () {
            //alert('AJAX Failed!');
            $('#correctness_value').hide();
            $('#snr_value').hide();
            $('#dsnr_value').hide();
            return true;
        }
    });
}

function ajaxDES(isProtein, number) {

    var filename = res.files.localopt;
    var jobID = res.jobID;
    $.ajax({
        url: "results/" + jobID + "/" + filename,
        type: "GET",
        headers: {
            "Authorization": "Basic " + btoa(sessionStorage.username + ":" + sessionStorage.password)
        },
        data: {
        },
        success: function (response) {
            response = response.replace(/</g, '&lt;');
            var obj = $.parseJSON(response);
            if (isProtein)
                configuration = obj['circuits_protein'][number];
            else
                configuration = obj['circuits_dna'][number];
            return true;
        },
        error: function () {
            $('#original_score_value').hide();
            return true;
        }
    });
}

function showPlasmid(element) {

    var jobID = res.jobID;
    var filename = $(element).html();

    $.ajax({
        url: "results/" + jobID + "/" + filename,
        type: "GET",
        headers: {
            "Authorization": "Basic " + btoa(sessionStorage.username + ":" + sessionStorage.password)
        },
        data: {
        },
        success: function (response) {
            $('#plasmid_text').text(response);
        },
        error: function () {
        }
    });
}




if (findBootstrapEnvironment() === "xs" || findBootstrapEnvironment() === "sm" || findBootstrapEnvironment() === "md") {
    $('#results_well').css('margin-top', '140px');
} else {
    $('#results_well').css('margin-top', '80px');
}

var mtop = Number($('#results_well').css('margin-top').replace('px', ''));

var pageHeight = jQuery(window).height();
var navHeight = pageHeight - 80 - mtop;
$(".scrolling").css("max-height", navHeight);
$("#results_well").css("max-height", navHeight);
$("#results_well").css("height", navHeight);

$(window).resize(function () {
    mtop = Number($('#results_well').css('margin-top').replace('px', ''));
    pageHeight = jQuery(window).height();
    navHeight = pageHeight - 80 - mtop;
    $(".scrolling").css("max-height", navHeight);
    $("#results_well").css("max-height", navHeight);
    $("#results_well").css("height", navHeight);

    if (findBootstrapEnvironment() === "xs" || findBootstrapEnvironment() === "sm" || findBootstrapEnvironment() === "md") {
        $('#results_well').css('margin-top', '140px');
    } else {
        $('#results_well').css('margin-top', '80px');
    }

});


$(window).bind('beforeunload', function () {
    sessionStorage.jobID = $('#result_pulldown').val();
    sessionStorage.results_page = JSON.stringify(res);
});