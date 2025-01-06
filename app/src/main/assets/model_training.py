#!/usr/bin/env python3
import tensorflow as tf
import numpy as np
import pandas as pd
from sklearn.preprocessing import StandardScaler
import os
import argparse

def load_real_data(csv_path):
    df = pd.read_csv(csv_path)

    # Add derived features
    df['games_experience'] = np.log1p(df['games_played'])
    df['score_consistency'] = df['consistency'] * df['avg_score']

    # Features
    feature_cols = ['games_played', 'win_rate', 'avg_score',
                   'consistency', 'games_experience', 'score_consistency']
    X = df[feature_cols].values

    # Labels
    y = df[['pred_win_rate', 'consistency', 'improvement', 'play_style']].values

    return X, y

def normalize_data(X):
    scaler = StandardScaler()
    X_scaled = scaler.fit_transform(X)
    return X_scaled

def create_model():
    model = tf.keras.Sequential([
        tf.keras.layers.Dense(12, activation='relu', input_shape=(6,)),
        tf.keras.layers.Dropout(0.2),
        tf.keras.layers.Dense(24, activation='relu'),
        tf.keras.layers.Dropout(0.2),
        tf.keras.layers.Dense(12, activation='relu'),
        tf.keras.layers.Dense(4, activation='sigmoid')
    ])

    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=0.001),
        loss='mse',
        metrics=['mae', 'mse']
    )
    return model

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--data_path', required=True, help='Path to training data CSV')
    args = parser.parse_args()

    # Load and preprocess data
    X, y = load_real_data(args.data_path)
    X = normalize_data(X)

    # Split data
    train_size = int(0.8 * len(X))
    X_train, X_val = X[:train_size], X[train_size:]
    y_train, y_val = y[:train_size], y[train_size:]

    # Create and train model
    model = create_model()

    early_stopping = tf.keras.callbacks.EarlyStopping(
        monitor='val_loss',
        patience=5,
        restore_best_weights=True
    )

    model.fit(
        X_train, y_train,
        validation_data=(X_val, y_val),
        epochs=50,
        batch_size=32,
        callbacks=[early_stopping],
        verbose=1
    )

    # Save model
    output_dir = os.path.dirname(args.data_path)
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    tflite_model = converter.convert()

    with open(os.path.join(output_dir, 'dice_stats_model.tflite'), 'wb') as f:
        f.write(tflite_model)

    print("Model trained and saved successfully")

if __name__ == "__main__":
    main()