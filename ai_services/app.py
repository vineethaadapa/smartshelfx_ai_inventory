from flask import Flask, request, jsonify
from flask_cors import CORS
import pandas as pd
import numpy as np
from sklearn.linear_model import LinearRegression
from datetime import datetime, timedelta
import mysql.connector 

def get_db_connection():
    return mysql.connector.connect(
        host="localhost",
        user="root",       
        password="root", 
        database="smartshelfx_db"   
    )
app = Flask(__name__)
CORS(app)

def predict_demand(history_data):
    # FIX 1: Handle empty data
    if not history_data or len(history_data) == 0:
        return [0]*7, []

    df = pd.DataFrame(history_data)
    df['date'] = pd.to_datetime(df['date'])
    df = df.sort_values('date')
    
    # FIX 2: Handle "Not enough data to draw a line"
    if len(df) < 2:
        avg_val = int(df['quantity'].mean())
        # Just return the average if we can't do math on it yet
        return [avg_val]*7, [{"date": "Future", "predicted_quantity": avg_val}]

    first_date = df['date'].min()
    df['day_index'] = (df['date'] - first_date).dt.days
    
    X = df[['day_index']].values
    y = df['quantity'].values
    
    model = LinearRegression()
    model.fit(X, y)
    
    last_day_index = df['day_index'].max()
    future_indices = np.array([i for i in range(last_day_index + 1, last_day_index + 8)]).reshape(-1, 1)
    
    predictions = model.predict(future_indices)
    
    # FIX 3: Flatten the predictions to a simple list
    final_predictions = np.maximum(0, np.round(predictions)).astype(int).flatten().tolist()
    
    last_date = df['date'].max()
    future_dates = [(last_date + timedelta(days=i)).strftime('%b %d') for i in range(1, 8)]
    
    prediction_table = [{"date": d, "predicted_quantity": v} for d, v in zip(future_dates, final_predictions)]
    return final_predictions, prediction_table

@app.route('/api/forecast_all', methods=['POST'])
def forecast_all():
    items = request.json.get('items', [])
    results = []

    for item in items:
        mock_history = [
            {
                "date": (datetime.now() - timedelta(days=i)).strftime('%Y-%m-%d'), 
                "quantity": int(np.random.randint(5, 15)) 
            } for i in range(14, 0, -1)
        ]
        predictions, _ = predict_demand(mock_history)
        flat_predictions = [int(p) for p in predictions]
        total_forecast = sum(flat_predictions)
        
        if int(item['quantity']) != 0:
            results.append({
                "sku": item['sku'],
                "name": item['name'],
                "forecast": total_forecast,
                "current_stock": int(item['quantity']),
                "chart_data": flat_predictions,
                "action": "Restock Immediately" if int(item['quantity']) < total_forecast else "Optimal"
            })
        
    return jsonify({"predictions": results})

@app.route('/api/inventory', methods=['POST'])
def update_stock():
    data = request.json
    sku = data.get('sku')
    quantity_to_add = data.get('quantityToAdd')
    product = Product.query.filter_by(sku=sku).first()
    
    if product:
        product.currentStock += quantity_to_add
        db.session.commit()
        return jsonify({"message": "Stock updated successfully"}), 200
    
    return jsonify({"error": "Product not found"}), 404


@app.route('/api/predict_for_vendor/<int:vendor_id>', methods=['GET'])
def predict_for_vendor(vendor_id):
    try:
        conn = get_db_connection()
        cursor = conn.cursor(dictionary=True)
    
        cursor.execute("SELECT name FROM products WHERE vendor_id = %s AND deleted = 0", (vendor_id,))
        products = cursor.fetchall()
        
        results = []
        for p in products:
            p_name = p['name']
            
            mock_history = [np.random.randint(1, 15) for _ in range(14)]
            prediction_val = float(sum(mock_history) / 2) 
            
            results.append({
                "productName": p_name,
                "predictedDemand": prediction_val,
                "predictionDate": datetime.now().strftime('%Y-%m-%d'),
                "vendorId": vendor_id
            })
            
        cursor.close()
        conn.close()
        return jsonify(results)
    except Exception as e:
        print(f"DEBUG FLASK ERROR: {e}")
        return jsonify([])


@app.route('/api/predict_for_manager/<int:wh_id>', methods=['GET'])
def predict_for_manager(wh_id):
    try:
        conn = get_db_connection()
        cursor = conn.cursor(dictionary=True)
        
        cursor.execute("SELECT id, name, vendor_id, price, sku FROM products WHERE deleted = 0")
        products = cursor.fetchall()
        print(f"DEBUG FIRST PRODUCT: {products[0]}")

        results = []
        for p in products:
            query = """
                SELECT SUM(quantity) as total_out 
                FROM stock_transactions 
                WHERE product_id = %s 
                AND warehouseId = %s 
                AND type = 'OUT' 
                AND timestamp >= DATE_SUB(NOW(), INTERVAL 30 DAY)
            """
            cursor.execute(query, (p['id'], wh_id))
            row = cursor.fetchone()
            total_sold = float(row['total_out']) if row['total_out'] else 0.0
            weekly_prediction = (total_sold / 30) * 7
            final_val = round(weekly_prediction * 1.1, 2)
            
            if final_val == 0:
                final_val = 5.0

            v_id = p.get('vendor_id')

            results.append({
                "id": p['id'],            
                "productId": p['id'],     
                "productName": p['name'],
                "sku": p['sku'],              
                "unitPrice": float(p['price']), 
                "predictedDemand": final_val,
                "vendorId": v_id,
                "predictionDate": datetime.now().strftime('%Y-%m-%d')
            })
            
        cursor.close()
        conn.close()
        return jsonify(results)
    except Exception as e:
        print(f"DEBUG FLASK MANAGER ERROR: {e}")
        return jsonify([])

if __name__ == '__main__':
    app.run(port=5001, debug=True)