import numpy as np
import heartpy as hp
import json

def hrv_analysis(data, sampling_rate):
    if len(data) == 0:
        return None, json.dumps({"error": "Input data is empty"}), json.dumps({"error": "Input data is empty"})

    data_np = np.array(data)

    # 使用Hampel濾波器進行初步處理
    data_np = hp.hampel_filter(data_np, filtsize=3)

    try:
        # 初步處理來獲取R點
        wd, m = hp.process(
            data_np,
            sample_rate=sampling_rate,
            calc_freq=True,
            clean_rr=True,
            clean_rr_method="iqr",
            reject_segmentwise=True,
        )
    except hp.exceptions.BadSignalWarning as e:
        return None, json.dumps({"error": str(e)}), json.dumps({"error": str(e)})

    # 獲取R點並計算中位數
    filtered_r_peaks = wd["peaklist"]
    r_values = data_np[filtered_r_peaks]
    median_r_value = np.median(r_values)

    # 如果中位數小於0.5，翻轉數據
    if abs(median_r_value) < 0.5:
        data_np = hp.flip_signal(data_np, enhancepeaks=False, keep_range=True)

        try:
            # 重新處理翻轉後的數據
            wd, m = hp.process(
                data_np,
                sample_rate=sampling_rate,
                calc_freq=True,
                clean_rr=True,
                clean_rr_method="iqr",
                reject_segmentwise=True,
            )
        except hp.exceptions.BadSignalWarning as e:
            return None, json.dumps({"error": str(e)}), json.dumps({"error": str(e)})

        # 更新R點和R值
        filtered_r_peaks = wd["peaklist"]
        r_values = data_np[filtered_r_peaks]

    filtered_r_peaks = [int(i) for i in filtered_r_peaks]
    r_values = [float(i) for i in r_values]

    r_peaks_data = {
        "r_peaks": filtered_r_peaks,
    }

    r_values_data = {"r_values": r_values}

    r_peaks_json = json.dumps(r_peaks_data)
    r_values_json = json.dumps(r_values_data)

    return m, r_peaks_json, r_values_json