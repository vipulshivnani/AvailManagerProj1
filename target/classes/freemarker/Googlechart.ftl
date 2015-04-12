<!DOCTYPE html>
<html>
<head>
    <title>My Blog</title>

</head>
<body>


<script type="text/javascript" src="https://www.google.com/jsapi?autoload={'modules':[{'name':'visualization','version':'1.1','packages':['line', 'corechart']}]}"></script>
<button id="change-chart">Change to Classic</button>
<br><br>
<div id="values">

<#list gcdata as mm >


${gcdata[3]?size}

</#list>
<#--<#list gcdata as mm >
    MaxCPU :${mm[1]}
    MaxMemory :${mm[2]}


</#list>
-->
</div>
<div id="classic" class="hide"></div>
<div id="material"></div>


<script type="application/javascript">
    google.load('visualization', '1.1', {packages: ['line', 'corechart']});
    google.setOnLoadCallback(drawChart);

    function drawChart() {

        var materialChart;
        var classicChart;
        var button = document.getElementById('change-chart');
        var materialDiv = document.getElementById('material');
        var classicDiv = document.getElementById('classic');

        var data = new google.visualization.DataTable();
        data.addColumn('number', 'Count');
        data.addColumn('number', 'MaxMemoryUsage');
        data.addColumn('number', 'MaxCPUusage');
        var counter=0;
        var newrow= new Array();
    <#list gcdata as mm >

    <#list mm as mmele>
        newrow.push(${mmele});
    </#list>
    data.addRow(newrow);
    newrow=[];

    </#list>



       /* data.addRows([
            [new Date(2014, 0),  120,  5.7],
            [new Date(2014, 1),   .4,  8.7],
            [new Date(2014, 2),   .5,   12],
            [new Date(2014, 3),  2.9, 15.3],
            [new Date(2014, 4),  6.3, 18.6],
            [new Date(2014, 5),    9, 20.9],
            [new Date(2014, 6), 10.6, 19.8],
            [new Date(2014, 7), 10.3, 16.6],
            [new Date(2014, 8),  7.4, 13.3],
            [new Date(2014, 9),  4.4,  9.9],
            [new Date(2014, 10), 1.1,  6.6],
            [new Date(2014, 11), -.2,  4.5]
        ]);

        data.addRows([
            [new Date(2015, 0),  -.5,  5.7],
            [new Date(2015, 1),   .4,  8.7],
            [new Date(2015, 2),   .5,   12],
            [new Date(2015, 3),  2.9, 15.3],
            [new Date(2015, 4),  150, 18.6],
            [new Date(2015, 5),    1000, 20.9]]);*/

        var materialOptions = {
            chart: {
                title: 'Average Temperatures and Daylight in Iceland Throughout the Year'
            },
            width: 900,
            height: 500,
            series: {
                // Gives each series an axis name that matches the Y-axis below.
                0: {axis: 'Temps'},
                1: {axis: 'Daylight'}
            },
            axes: {
                // Adds labels to each axis; they don't have to match the axis names.
                y: {
                    Temps: {label: 'Temps (Celsius)'},
                    Daylight: {label: 'Daylight'}
                }
            }
        };

        var classicOptions = {
            title: 'Average Temperatures and Daylight in Iceland Throughout the Year',
            width: 900,
            height: 500,
            // Gives each series an axis that matches the vAxes number below.
            series: {
                0: {targetAxisIndex: 0},
                1: {targetAxisIndex: 1}
            },
            vAxes: {
                // Adds titles to each axis.
                0: {title: 'Temps (Celsius)'},
                1: {title: 'Daylight'}
            },
            hAxis: {
                ticks: [new Date(2014, 0), new Date(2014, 1), new Date(2014, 2), new Date(2014, 3),
                    new Date(2014, 4),  new Date(2014, 5), new Date(2014, 6), new Date(2014, 7),
                    new Date(2014, 8), new Date(2014, 9), new Date(2014, 10), new Date(2014, 11)
                ]
            },
            vAxis: {
                viewWindow: {
                    max: 1000
                }
            }
        };

        materialChart = new google.charts.Line(materialDiv);
        classicChart = new google.visualization.LineChart(classicDiv);

        classicChart.draw(data, classicOptions);
        materialChart.draw(data, materialOptions);

        button.onclick = function () {
            materialDiv.classList.toggle('hide');
            classicDiv.classList.toggle('hide');

            if (materialDiv.classList.contains('hide')) {
                button.innerText = 'Change to Material';
            } else {
                button.innerText = 'Change to Classic';
            }

        };
    }
</script>



</body>
</html>


