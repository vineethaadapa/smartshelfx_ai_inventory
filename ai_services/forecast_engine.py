import pandas as pd
import numpy as np
from sklearn.linear_model import LinearRegression
from datetime import datetime, timedelta

def predict_demand(history_data):
    df = pd.DataFrame(history_data)
    df['date'] = pd.to_datetime(df['date'])
    df = df.sort_values('date')
    first_date = df['date'].min()
    df['day_index'] = (df['date'] - first_date).dt.days
    
    X = df[['day_index']].values
    y = df['quantity'].values
    
    model = LinearRegression()
    model.fit(X, y) 
    
    last_day_index = df['day_index'].max()
    future_indices = np.array([i for i in range(last_day_index + 1, last_day_index + 8)]).reshape(-1, 1)
    predictions = model.predict(future_indices)
    final_predictions = np.maximum(0, np.round(predictions)).astype(int).tolist()
    
    last_date = df['date'].max()
    future_dates = [(last_date + timedelta(days=i)).strftime('%Y-%m-%d') for i in range(1, 8)]
    
    prediction_table = [{"date": d, "predicted_quantity": v} for d, v in zip(future_dates, final_predictions)]
    return final_predictions, prediction_table