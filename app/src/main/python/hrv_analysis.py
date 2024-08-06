import neurokit2 as nk
import numpy as np
import json

def calculate_angle(point1, point2, point3):
    a = np.array(point1)
    b = np.array(point2)
    c = np.array(point3)
    ba = a - b
    bc = c - b
    cos_angle = np.dot(ba, bc) / (np.linalg.norm(ba) * np.linalg.norm(bc))
    angle = np.arccos(np.clip(cos_angle, -1.0, 1.0))
    return np.degrees(angle)

def hrv_analysis(data, sampling_rate):
    data_np = np.array(data)
    data_np = data_np[~np.isnan(data_np)]

    if len(data_np) == 0:
        return (
            None,
            json.dumps({"error": "Input data is empty after removing NaN values"}),
            json.dumps({"error": "Input data is empty after removing NaN values"}),
        )

    try:
        signals, info = nk.ecg_process(data_np, sampling_rate=sampling_rate)
        ecg_signal = signals["ECG_Clean"]
        rpeaks = info["ECG_R_Peaks"]
        signal_delineated, waves_delineated = nk.ecg_delineate(
            ecg_signal, rpeaks, sampling_rate=sampling_rate, method="cwt" , scale=0.5
        )

        p_peaks = waves_delineated["ECG_P_Peaks"]
        q_peaks = waves_delineated["ECG_Q_Peaks"]
        r_peaks = info["ECG_R_Peaks"]
        s_peaks = waves_delineated["ECG_S_Peaks"]
        t_peaks = waves_delineated["ECG_T_Peaks"]

        pqr_angles = []
        qrs_angles = []
        rst_angles = []

        for i in range(len(r_peaks) - 1):
            if not np.isnan(p_peaks[i]) and not np.isnan(q_peaks[i]) and not np.isnan(r_peaks[i]):
                angle_pqr = calculate_angle(
                    (p_peaks[i], data_np[int(p_peaks[i])]),
                    (q_peaks[i], data_np[int(q_peaks[i])]),
                    (r_peaks[i], data_np[int(r_peaks[i])])
                )
                pqr_angles.append(angle_pqr)

            if not np.isnan(q_peaks[i]) and not np.isnan(r_peaks[i]) and not np.isnan(s_peaks[i]):
                angle_qrs = calculate_angle(
                    (q_peaks[i], data_np[int(q_peaks[i])]),
                    (r_peaks[i], data_np[int(r_peaks[i])]),
                    (s_peaks[i], data_np[int(s_peaks[i])])
                )
                qrs_angles.append(angle_qrs)

            if not np.isnan(r_peaks[i]) and not np.isnan(s_peaks[i]) and not np.isnan(t_peaks[i]):
                angle_rst = calculate_angle(
                    (r_peaks[i], data_np[int(r_peaks[i])]),
                    (s_peaks[i], data_np[int(s_peaks[i])]),
                    (t_peaks[i], data_np[int(t_peaks[i])])
                )
                rst_angles.append(angle_rst)

        median_pqr_angle = np.average(pqr_angles)
        median_qrs_angle = np.average(qrs_angles)
        median_rst_angle = np.average(rst_angles)

        t_onsets = waves_delineated["ECG_T_Onsets"]
        t_peaks = waves_delineated["ECG_T_Peaks"]
        t_offsets = waves_delineated["ECG_T_Offsets"]

        t_wave_heights = []
        t_wave_durations = []
        for onset, peak, offset in zip(t_onsets, t_peaks, t_offsets):
            if not np.isnan(onset) and not np.isnan(peak) and not np.isnan(offset):
                onset_amp = data_np[int(onset)]
                peak_amp = data_np[int(peak)]
                offset_amp = data_np[int(offset)]
                height = peak_amp - ((onset_amp + offset_amp) / 2)
                duration = offset - onset
                t_wave_heights.append(height)
                t_wave_durations.append(duration)

        triangle_areas = [
            0.5 * duration * height
            for duration, height in zip(t_wave_durations, t_wave_heights)
        ]

        median_triangle_area = np.median(triangle_areas)
        median_t_wave_height = np.median(t_wave_heights)

    except Exception as e:
        return None, json.dumps({"error": str(e)}), json.dumps({"error": str(e)})

    r_peaks = info["ECG_R_Peaks"]

    if len(r_peaks) == 0:
        return (
            None,
            json.dumps({"error": "No R peaks found"}),
            json.dumps({"error": "No R peaks found"}),
        )

    r_values = data_np[r_peaks]

    if np.isnan(r_values).any():
        r_values = r_values[~np.isnan(r_values)]

    if len(r_values) == 0:
        return (
            None,
            json.dumps({"error": "All R values are NaN"}),
            json.dumps({"error": "All R values are NaN"}),
        )

    try:
        hrv_metrics = nk.hrv(signals, sampling_rate=sampling_rate)
    except Exception as e:
        return None, json.dumps({"error": str(e)}), json.dumps({"error": str(e)})

    try:
        bpm = 60000 / hrv_metrics["HRV_MeanNN"].iloc[0]
        mean_nn = hrv_metrics["HRV_MeanNN"].iloc[0]
        sdnn = hrv_metrics["HRV_SDNN"].iloc[0]
        sdsd = hrv_metrics["HRV_SDSD"].iloc[0]
        rmssd = hrv_metrics["HRV_RMSSD"].iloc[0]
        pnn20 = hrv_metrics["HRV_pNN20"].iloc[0]
        pnn50 = hrv_metrics["HRV_pNN50"].iloc[0]
        hr_mad = hrv_metrics["HRV_MadNN"].iloc[0]
        sd1 = hrv_metrics["HRV_SD1"].iloc[0]
        sd2 = hrv_metrics["HRV_SD2"].iloc[0]
        sd1_sd2 = hrv_metrics["HRV_SD1SD2"].iloc[0]
        ap_en = hrv_metrics["HRV_ApEn"].iloc[0]
        shan_en = hrv_metrics["HRV_ShanEn"].iloc[0]

        score = 0
        if rmssd / mean_nn > 0.115:
            score += 32
        if shan_en > 5.4:
            score += 27
        if pnn50 > 40:
            score += 24
        if sdnn > 80:
            score += 16
        af = 1 if score > 50 else 0

    except Exception as e:
        return None, json.dumps({"error": str(e)}), json.dumps({"error": str(e)})

    features = {
        "bpm": bpm,
        "mean_nn": mean_nn,
        "sdnn": sdnn,
        "sdsd": sdsd,
        "rmssd": rmssd,
        "pnn20": pnn20,
        "pnn50": pnn50,
        "hr_mad": hr_mad,
        "sd1": sd1,
        "sd2": sd2,
        "sd1/sd2": sd1_sd2,
        "ap_en": ap_en,
        "shan_en": shan_en,
        "af": float(af),
        "t_area": median_triangle_area,
        "t_height": median_t_wave_height,
        "pqr_angle": median_pqr_angle,
        "qrs_angle": median_qrs_angle,
        "rst_angle": median_rst_angle,
        "t_onsets": [int(onset) for onset in t_onsets if not np.isnan(onset)],
        "t_peaks": [int(peak) for peak in t_peaks if not np.isnan(peak)],
        "t_offsets": [int(offset) for offset in t_offsets if not np.isnan(offset)],
    }

    filtered_r_peaks = [int(i) for i in r_peaks if not np.isnan(i)]
    r_values = [float(i) for i in r_values if not np.isnan(i)]

    r_peaks_data = {"r_peaks": filtered_r_peaks}
    r_values_data = {"r_values": r_values}

    signals_dict = {key: signals[key].tolist() for key in signals.columns}
    signal_json = json.dumps(signals_dict)

    r_peaks_json = json.dumps(r_peaks_data)
    r_values_json = json.dumps(r_values_data)

    return features, r_peaks_json, r_values_json, signal_json
