import neurokit2 as nk
import numpy as np
import json

def hrv_analysis(data, sampling_rate):
    data_np = np.array(data)
    data_np = data_np[~np.isnan(data_np)]

    if len(data_np) == 0:
        return json.dumps({"error": "Input data is empty after removing NaN values"})

    try:
        # 进行ECG处理并打点
        signals, info = nk.ecg_process(data_np, sampling_rate=sampling_rate)
        ecg_signal = signals["ECG_Clean"]
        rpeaks = info["ECG_R_Peaks"]

        # 使用cwt方法进行波形标定
        _, waves_delineated = nk.ecg_delineate(ecg_signal, rpeaks, sampling_rate=sampling_rate, method="cwt")

        # 提取PQRST点位
        p_peaks = waves_delineated.get("ECG_P_Peaks", [])
        q_peaks = waves_delineated.get("ECG_Q_Peaks", [])
        r_peaks = info["ECG_R_Peaks"]
        s_peaks = waves_delineated.get("ECG_S_Peaks", [])
        t_peaks = waves_delineated.get("ECG_T_Peaks", [])
        t_onsets = waves_delineated.get("ECG_T_Onsets", [])
        t_offsets = waves_delineated.get("ECG_T_Offsets", [])

        # 提取HRV参数
        hrv_metrics = nk.hrv(signals, sampling_rate=sampling_rate)

        # 计算HRV特征
        bpm = 60000 / hrv_metrics["HRV_MeanNN"].iloc[0]
        # mean_nn = hrv_metrics["HRV_MeanNN"].iloc[0]
        # sdnn = hrv_metrics["HRV_SDNN"].iloc[0]
        # sdsd = hrv_metrics["HRV_SDSD"].iloc[0]
        # rmssd = hrv_metrics["HRV_RMSSD"].iloc[0]
        # pnn20 = hrv_metrics["HRV_pNN20"].iloc[0]
        # pnn50 = hrv_metrics["HRV_pNN50"].iloc[0]
        # hr_mad = hrv_metrics["HRV_MadNN"].iloc[0]
        # sd1 = hrv_metrics["HRV_SD1"].iloc[0]
        # sd2 = hrv_metrics["HRV_SD2"].iloc[0]
        # sd1_sd2 = hrv_metrics["HRV_SD1SD2"].iloc[0]
        # ap_en = hrv_metrics["HRV_ApEn"].iloc[0]
        # shan_en = hrv_metrics["HRV_ShanEn"].iloc[0]
        # iqrnn = hrv_metrics["HRV_IQRNN"].iloc[0]
        # mcvnn = hrv_metrics["HRV_MCVNN"].iloc[0]
        # pip = hrv_metrics["HRV_PIP"].iloc[0]
        # hfd = hrv_metrics["HRV_HFD"].iloc[0]
        # lzc = hrv_metrics["HRV_LZC"].iloc[0]

        features = {
            "bpm": bpm,
            # "mean_nn": mean_nn,
            # "sdnn": sdnn,
            # "sdsd": sdsd,
            # "rmssd": rmssd,
            # "pnn20": pnn20,
            # "pnn50": pnn50,
            # "hr_mad": hr_mad,
            # "sd1": sd1,
            # "sd2": sd2,
            # "sd1/sd2": sd1_sd2,
            # "ap_en": ap_en,
            # "shan_en": shan_en,
            # "iqrnn": iqrnn,
            # "mcvnn": mcvnn,
            # "pip": pip,
            # "hfd": hfd,
            # "lzc": lzc
        }

        # 返回PQRST点位和其他点位的JSON
        pqrst_points = {
            "clean_signal": ecg_signal.tolist(),
            # "p_peaks": [int(i) for i in p_peaks if not np.isnan(i)],
            # "q_peaks": [int(i) for i in q_peaks if not np.isnan(i)],
            "r_peaks": [int(i) for i in r_peaks if not np.isnan(i)],
            # "s_peaks": [int(i) for i in s_peaks if not np.isnan(i)],
            # "t_peaks": [int(i) for i in t_peaks if not np.isnan(i)],
            # "t_onsets": [int(i) for i in t_onsets if not np.isnan(i)],
            # "t_offsets": [int(i) for i in t_offsets if not np.isnan(i)] 
        }

        pqrst_json = json.dumps(pqrst_points)
        features_json = json.dumps(features)

        return pqrst_json, features_json

    except Exception as e:
        return json.dumps({"error": str(e)})
