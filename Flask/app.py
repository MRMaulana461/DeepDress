import os
import logging
import traceback
import pickle
import joblib
import numpy as np
from flask import Flask, request, jsonify
from tensorflow.keras.models import load_model
from tensorflow.keras.preprocessing import image
from werkzeug.utils import secure_filename

# ======================================
# CONFIG
# ======================================
app = Flask(__name__)
logging.basicConfig(level=logging.INFO)

UPLOAD_FOLDER = "uploads"
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg'}
WINDOW_SIZE = 60  # LSTM window size

app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER
os.makedirs(UPLOAD_FOLDER, exist_ok=True)

# ======================================
# MODEL VARIABLES
# ======================================
trend_model = None
scaler = None
img_model = None
le_gender = None
le_usage = None
le_article = None

# ======================================
# UTILS
# ======================================
def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

def prepare_image(img_path):
    img = image.load_img(img_path, target_size=(256, 256))  # Sesuaikan dengan model CNN kamu
    img_array = image.img_to_array(img)
    img_array = img_array / 255.0
    return np.expand_dims(img_array, axis=0)

# ======================================
# LOAD MODELS
# ======================================
def load_trend_model():
    global trend_model
    if not os.path.exists("world_best_model2.h5"):
        logging.error("Trend model file not found!")
        return False
    try:
        trend_model = load_model("world_best_model2.h5")
        logging.info("Trend model loaded successfully")
        return True
    except Exception as e:
        logging.error(f"Failed to load trend model: {str(e)}")
        return False

def load_scaler():
    global scaler
    if not os.path.exists("scaler.pkl"):
        logging.error("Scaler file not found!")
        return False
    try:
        scaler = joblib.load("scaler.pkl")
        logging.info("Scaler loaded successfully")
        return True
    except Exception as e:
        logging.error(f"Failed to load scaler: {str(e)}")
        return False

def load_image_model():
    global img_model, le_gender, le_usage, le_article
    try:
        img_model = load_model("uploads/test 2/best_robust_model.h5")
        le_gender = pickle.load(open("uploads/test 2/le_gender.pkl", "rb"))
        le_usage = pickle.load(open("uploads/test 2/le_usage.pkl", "rb"))
        le_article = pickle.load(open("uploads/test 2/le_article.pkl", "rb"))
        logging.info("Image model & label encoders loaded successfully")
        return True
    except Exception as e:
        logging.error(f"Failed to load image model: {str(e)}")
        return False

# ======================================
# ENDPOINTS
# ======================================
@app.route("/predict_trend", methods=["POST"])
def predict_trend():
    if trend_model is None or scaler is None:
        return jsonify({"error": "Trend model or scaler not loaded"}), 500

    if not request.is_json:
        return jsonify({"error": "Request must be JSON"}), 400

    data = request.get_json()
    days = data.get("days", None)

    if days is None or not isinstance(days, int) or days <= 0 or days > 60:
        return jsonify({"error": "Invalid or missing 'days' parameter (1–60 allowed)"}), 400

    try:
        initial_sequence = np.linspace(0, 1, WINDOW_SIZE).tolist()
        sequence = initial_sequence.copy()
        predictions = []

        for _ in range(days):
            input_array = np.array(sequence[-WINDOW_SIZE:]).reshape((1, WINDOW_SIZE, 1))
            pred_scaled = trend_model.predict(input_array, verbose=0)[0][0]
            pred_original = scaler.inverse_transform([[pred_scaled]])[0][0]
            predictions.append(float(pred_original))
            sequence.append(pred_scaled)

        return jsonify({
            "prediction": predictions,
            "days_predicted": days,
            "status": "success"
        })

    except Exception as e:
        logging.error(f"Prediction error: {str(e)}")
        return jsonify({"error": str(e)}), 500

@app.route("/predict_image", methods=["POST"])
def predict_image():
    if img_model is None:
        return jsonify({'error': 'Image model not loaded'}), 500

    if 'image' not in request.files:
        return jsonify({'error': 'No image uploaded'}), 400
    
    img = request.files['image']
    
    if img.filename == '':
        return jsonify({'error': 'No selected file'}), 400
    
    if not allowed_file(img.filename):
        return jsonify({'error': 'File type not allowed. Allowed types: png, jpg, jpeg'}), 400
    
    filename = secure_filename(img.filename)
    img_path = os.path.join(app.config['UPLOAD_FOLDER'], filename)
    img.save(img_path)

    try:
        img_array = prepare_image(img_path)
        preds = img_model.predict(img_array)

        if isinstance(preds, list) and len(preds) == 3:
            preds_gender = preds[0][0]
            preds_usage = preds[1][0]
            preds_article = preds[2][0]

            gender_pred = le_gender.inverse_transform([np.argmax(preds_gender)])[0]
            usage_pred = le_usage.inverse_transform([np.argmax(preds_usage)])[0]
            article_pred = le_article.inverse_transform([np.argmax(preds_article)])[0]

        else:
            preds = preds[0]
            gender_pred = le_gender.inverse_transform([np.argmax(preds[0:2])])[0]
            usage_pred = le_usage.inverse_transform([np.argmax(preds[2:5])])[0]
            article_pred = le_article.inverse_transform([np.argmax(preds[5:])])[0]

        return jsonify({
            'gender': gender_pred,
            'usage': usage_pred,
            'article': article_pred
        })

    except Exception as e:
        traceback.print_exc()
        return jsonify({'error': str(e)}), 500
    finally:
        try:
            os.remove(img_path)
        except Exception:
            pass

@app.route("/health", methods=["GET"])
def health():
    return jsonify({
        "status": "healthy",
        "trend_model_loaded": trend_model is not None,
        "scaler_loaded": scaler is not None,
        "image_model_loaded": img_model is not None
    })

# ======================================
# MAIN
# ======================================
if __name__ == '__main__':
    trend_ok = load_trend_model()
    scaler_ok = load_scaler()
    image_ok = load_image_model()

    if trend_ok and scaler_ok and image_ok:
        app.run(debug=True, host="0.0.0.0", port=5000)
    else:
        logging.error("App failed to start due to missing model or scaler or image model.")
