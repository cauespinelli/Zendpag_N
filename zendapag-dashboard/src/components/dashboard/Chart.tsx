import React from 'react';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  ArcElement,
  Title,
  Tooltip,
  Legend,
  Filler
} from 'chart.js';
import { Line, Doughnut, Bar } from 'react-chartjs-2';

// Registrar componentes do Chart.js
ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  ArcElement,
  Title,
  Tooltip,
  Legend,
  Filler
);

interface ChartProps {
  type: 'line' | 'doughnut' | 'bar';
}

export default function Chart({ type }: ChartProps) {
  // Configurações base
  const baseOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: type !== 'line',
        position: 'bottom' as const,
        labels: {
          usePointStyle: true,
          padding: 20,
          font: {
            size: 12,
            family: 'Inter, sans-serif'
          }
        }
      },
      tooltip: {
        backgroundColor: 'rgba(26, 31, 54, 0.9)',
        padding: 12,
        borderColor: 'rgba(255, 255, 255, 0.1)',
        borderWidth: 1,
        titleFont: {
          size: 14,
          weight: 'bold' as const
        },
        bodyFont: {
          size: 13
        },
        displayColors: true,
        usePointStyle: true
      }
    }
  };

  // Line Chart - Revenue
  if (type === 'line') {
    const data = {
      labels: ['01/11', '05/11', '10/11', '15/11', '20/11', '25/11', '30/11'],
      datasets: [
        {
          label: 'Receita',
          data: [12500, 19800, 15400, 22300, 18900, 25600, 28400],
          borderColor: 'rgb(0, 102, 255)',
          backgroundColor: 'rgba(0, 102, 255, 0.1)',
          fill: true,
          tension: 0.4,
          pointRadius: 4,
          pointHoverRadius: 6,
          pointBackgroundColor: 'rgb(0, 102, 255)',
          pointBorderColor: '#fff',
          pointBorderWidth: 2
        }
      ]
    };

    const options = {
      ...baseOptions,
      scales: {
        y: {
          beginAtZero: true,
          grid: {
            color: 'rgba(0, 0, 0, 0.05)',
            drawBorder: false
          },
          ticks: {
            callback: function(value: any) {
              return 'R$ ' + value.toLocaleString('pt-BR');
            },
            font: {
              size: 11
            }
          }
        },
        x: {
          grid: {
            display: false,
            drawBorder: false
          },
          ticks: {
            font: {
              size: 11
            }
          }
        }
      }
    };

    return (
      <div style={{ height: '280px' }}>
        <Line data={data} options={options} />
      </div>
    );
  }

  // Doughnut Chart - Payment Methods
  if (type === 'doughnut') {
    const data = {
      labels: ['PIX', 'Cartão de Crédito', 'Cartão de Débito', 'Boleto'],
      datasets: [
        {
          data: [45, 30, 15, 10],
          backgroundColor: [
            'rgb(0, 102, 255)',
            'rgb(6, 182, 212)',
            'rgb(16, 185, 129)',
            'rgb(245, 158, 11)'
          ],
          borderWidth: 0,
          hoverOffset: 8
        }
      ]
    };

    const options = {
      ...baseOptions,
      cutout: '65%',
      plugins: {
        ...baseOptions.plugins,
        legend: {
          ...baseOptions.plugins.legend,
          display: true
        }
      }
    };

    return (
      <div style={{ height: '280px' }}>
        <Doughnut data={data} options={options} />
      </div>
    );
  }

  // Bar Chart - Transactions by Day
  if (type === 'bar') {
    const data = {
      labels: ['Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb', 'Dom'],
      datasets: [
        {
          label: 'Transações',
          data: [320, 450, 380, 520, 490, 280, 150],
          backgroundColor: 'rgba(0, 102, 255, 0.8)',
          borderRadius: 6,
          maxBarThickness: 60
        }
      ]
    };

    const options = {
      ...baseOptions,
      scales: {
        y: {
          beginAtZero: true,
          grid: {
            color: 'rgba(0, 0, 0, 0.05)',
            drawBorder: false
          },
          ticks: {
            font: {
              size: 11
            }
          }
        },
        x: {
          grid: {
            display: false,
            drawBorder: false
          },
          ticks: {
            font: {
              size: 11
            }
          }
        }
      }
    };

    return (
      <div style={{ height: '280px' }}>
        <Bar data={data} options={options} />
      </div>
    );
  }

  return null;
}
