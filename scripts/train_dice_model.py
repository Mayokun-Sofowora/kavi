import tensorflow as tf
import numpy as np
import os

# Define the path for the TFLite model
output_path = os.path.join(os.path.dirname(__file__), "../app/src/main/assets/")
os.makedirs(output_path, exist_ok=True)

def generate_training_data(n_samples=1000):
    # Generate features
    X = np.random.rand(n_samples, 4)
    X[:, 0] *= 100  # games_played (0-100)
    X[:, 1] *= 1.0  # win_rate (0-1)
    X[:, 2] *= 1000  # avg_score (0-1000)
    X[:, 3] *= 1.0  # consistency (0-1)

    # Generate labels
    y = np.zeros((n_samples, 4))
    y[:, 0] = 0.6 * X[:, 1] + 0.4 * X[:, 3]  # predicted win rate
    y[:, 1] = X[:, 3]  # consistency
    y[:, 2] = 1.0 - (X[:, 0] / 100)  # improvement potential
    y[:, 3] = 0.4 * X[:, 1] + 0.6 * X[:, 2] / 1000  # play style

    return X, y

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

# Generate data
X, y = generate_training_data()

# Split data manually
train_size = int(0.8 * len(X))
X_train, X_val = X[:train_size], X[train_size:]
y_train, y_val = y[:train_size], y[train_size:]

# Create and train model
model = create_model()
model.fit(
    X_train, y_train,
    validation_data=(X_val, y_val),
    epochs=50,
    batch_size=32,
    verbose=1
)

# Convert to TFLite
converter = tf.lite.TFLiteConverter.from_keras_model(model)
tflite_model = converter.convert()

# Add metadata
with tf.io.gfile.GFile(os.path.join(output_path, 'dice_stats_model.tflite'), 'wb') as f:
    f.write(tflite_model)

print(f"Model saved to {output_path}")

# Optional: Test the model
test_input = np.array([[50, 0.7, 500, 0.8]])  # Example input
predictions = model.predict(test_input)
print("\nTest prediction:", predictions)