import tensorflow as tf
import numpy as np

# Create synthetic training data
def generate_training_data(n_samples=1000):
    # Features: [games_played, win_rate, avg_score, consistency]
    X = np.random.rand(n_samples, 4)
    X[:, 0] *= 100  # games_played (0-100)
    X[:, 1] *= 1.0  # win_rate (0-1)
    X[:, 2] *= 1000  # avg_score (0-1000)
    X[:, 3] *= 1.0  # consistency (0-1)
    
    # Labels: [predicted_win_rate, consistency, improvement, play_style]
    y = np.zeros((n_samples, 4))
    y[:, 0] = 0.6 * X[:, 1] + 0.4 * X[:, 3]  # predicted win rate
    y[:, 1] = X[:, 3]  # consistency
    y[:, 2] = 1.0 - (X[:, 0] / 100)  # improvement potential
    y[:, 3] = 0.4 * X[:, 1] + 0.6 * X[:, 2] / 1000  # play style
    
    return X, y

# Create and train model
def create_model():
    model = tf.keras.Sequential([
        tf.keras.layers.Dense(8, activation='relu', input_shape=(4,)),
        tf.keras.layers.Dense(16, activation='relu'),
        tf.keras.layers.Dense(8, activation='relu'),
        tf.keras.layers.Dense(4, activation='sigmoid')
    ])
    
    model.compile(
        optimizer='adam',
        loss='mse',
        metrics=['mae']
    )
    return model

# Train and export model
X_train, y_train = generate_training_data()
model = create_model()
model.fit(X_train, y_train, epochs=50, batch_size=32, verbose=1)

# Convert to TFLite
converter = tf.lite.TFLiteConverter.from_keras_model(model)
tflite_model = converter.convert()

# Save the model
with open('../app/src/main/assets/dice_stats_model.tflite', 'wb') as f:
    f.write(tflite_model) 