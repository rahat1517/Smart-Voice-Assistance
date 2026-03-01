import json, re
import numpy as np
import pandas as pd
import tensorflow as tf
from tensorflow.keras.preprocessing.sequence import pad_sequences

MAX_LEN = 20

def normalize(s):
    s = str(s).lower().strip()
    s = re.sub(r"[।?!,;:]", " ", s)
    s = re.sub(r"\s+", " ", s).strip()
    return s

# 1) load data
df = pd.read_csv("nlu_train.csv")
df["text"] = df["text"].apply(normalize)

texts = df["text"].tolist()
labels = df["label"].tolist()

# 2) build label encoder
unique_labels = sorted(list(set(labels)))
label2idx = {l:i for i,l in enumerate(unique_labels)}
idx2label = {i:l for l,i in label2idx.items()}
y = np.array([label2idx[l] for l in labels], dtype=np.int32)

# 3) build vocab
word2idx = {"<PAD>":0, "<UNK>":1}
for t in texts:
    for w in t.split():
        if w not in word2idx:
            word2idx[w] = len(word2idx)

def encode(text):
    tokens = text.split()
    ids = [word2idx.get(w, 1) for w in tokens]
    return ids

X = [encode(t) for t in texts]
X = pad_sequences(X, maxlen=MAX_LEN, padding="post", truncating="post", value=0)
X = X.astype(np.int32)

num_classes = len(unique_labels)
vocab_size = len(word2idx)

# 4) model (simple and fast)
model = tf.keras.Sequential([
    tf.keras.layers.Embedding(vocab_size, 32, input_length=MAX_LEN),
    tf.keras.layers.GlobalAveragePooling1D(),
    tf.keras.layers.Dense(64, activation="relu"),
    tf.keras.layers.Dense(num_classes, activation="softmax")
])

model.compile(
    optimizer="adam",
    loss="sparse_categorical_crossentropy",
    metrics=["accuracy"]
)

model.fit(X, y, epochs=25, batch_size=16, validation_split=0.2)

# 5) export vocab + labels
with open("vocab.json", "w", encoding="utf-8") as f:
    json.dump({"word2idx": word2idx, "vocab_size": vocab_size, "max_len": MAX_LEN}, f, ensure_ascii=False)

with open("label_encoder.json", "w", encoding="utf-8") as f:
    json.dump({"label2idx": label2idx, "idx2label": idx2label, "num_classes": num_classes}, f, ensure_ascii=False)

# 6) convert to tflite (✅ ANDROID-COMPATIBLE)
converter = tf.lite.TFLiteConverter.from_keras_model(model)

# ✅ force built-in ops only (compat)
converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS]

# ✅ optimizations (safe)
converter.optimizations = [tf.lite.Optimize.DEFAULT]

# ✅ ensure input/output types are stable
converter.inference_input_type = tf.int32
converter.inference_output_type = tf.float32

tflite_model = converter.convert()

with open("voice_intent_model.tflite", "wb") as f:
    f.write(tflite_model)

print("✅ Export done!")
print("vocab_size =", vocab_size)
print("num_classes =", num_classes)
print("TensorFlow version =", tf.__version__)
