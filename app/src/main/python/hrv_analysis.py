import neurokit2 as nk
import numpy as np
import json

def hrv_analysis(data, sampling_rate):
    data_np = np.array(data)
    # 移除NaN值
    data_np = data_np[~np.isnan(data_np)]

    if len(data_np) == 0:
        return None, json.dumps({"error": "Input data is empty after removing NaN values"}), json.dumps({"error": "Input data is empty after removing NaN values"})

    # 使用NeuroKit2進行R點檢測
    try:
        # 檢測R點
        signals, info = nk.ecg_process(data_np, sampling_rate=sampling_rate)
    except Exception as e:
        return None, json.dumps({"error": str(e)}), json.dumps({"error": str(e)})

    # 獲取R點
    r_peaks = info["ECG_R_Peaks"]

    if len(r_peaks) == 0:
        return (
            None,
            json.dumps({"error": "No R peaks found"}),
            json.dumps({"error": "No R peaks found"}),
        )

    # 獲取R值
    r_values = data_np[r_peaks]

    if np.isnan(r_values).any():
        r_values = r_values[~np.isnan(r_values)]

    if len(r_values) == 0:
        return (
            None,
            json.dumps({"error": "All R values are NaN"}),
            json.dumps({"error": "All R values are NaN"}),
        )

    # 計算HRV指標
    try:
        hrv_metrics = nk.hrv(signals, sampling_rate=sampling_rate)
    except Exception as e:
        return None, json.dumps({"error": str(e)}), json.dumps({"error": str(e)})

    # 提取所需的HRV特徵
    try:
        bpm = 60000 / hrv_metrics['HRV_MeanNN'].iloc[0]
        ibi = hrv_metrics['HRV_MeanNN'].iloc[0]
        sdnn = hrv_metrics['HRV_SDNN'].iloc[0]
        sdsd = hrv_metrics['HRV_SDSD'].iloc[0]
        rmssd = hrv_metrics['HRV_RMSSD'].iloc[0]
        pnn20 = hrv_metrics['HRV_pNN20'].iloc[0]
        pnn50 = hrv_metrics['HRV_pNN50'].iloc[0]
        hr_mad = hrv_metrics['HRV_MadNN'].iloc[0]
        sd1 = hrv_metrics['HRV_SD1'].iloc[0]
        sd2 = hrv_metrics['HRV_SD2'].iloc[0]
        sd1_sd2 = hrv_metrics['HRV_SD1SD2'].iloc[0]
    except Exception as e:
        return None, json.dumps({"error": str(e)}), json.dumps({"error": str(e)})

    features = {
        'bpm': bpm,
        'ibi': ibi,
        'sdnn': sdnn,
        'sdsd': sdsd,
        'rmssd': rmssd,
        'pnn20': pnn20,
        'pnn50': pnn50,
        'hr_mad': hr_mad,
        'sd1': sd1,
        'sd2': sd2,
        'sd1/sd2': sd1_sd2,
    }

    filtered_r_peaks = [int(i) for i in r_peaks if not np.isnan(i)]
    r_values = [float(i) for i in r_values if not np.isnan(i)]

    r_peaks_data = {"r_peaks": filtered_r_peaks}
    r_values_data = {"r_values": r_values}
    signals_data = signals.to_dict()  # 將處理好的信號轉換為字典

    r_peaks_json = json.dumps(r_peaks_data)
    r_values_json = json.dumps(r_values_data)

    return features, r_peaks_json, r_values_json, signals_data
