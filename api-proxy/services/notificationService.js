const admin = require('firebase-admin');
const { createClient } = require('@supabase/supabase-js');

const notificationsEnabled = process.env.NOTIFICATION_ENABLED !== 'false';

let firebaseApp;
function getFirebaseApp() {
  if (firebaseApp || !notificationsEnabled) {
    return firebaseApp;
  }

  const serviceAccountJson = process.env.FIREBASE_SERVICE_ACCOUNT_KEY;
  if (!serviceAccountJson) {
    console.warn('[Notifications] FIREBASE_SERVICE_ACCOUNT_KEY not set. Notifications disabled.');
    return null;
  }

  try {
    const serviceAccount = JSON.parse(serviceAccountJson);
    firebaseApp = admin.initializeApp({
      credential: admin.credential.cert(serviceAccount)
    });
    return firebaseApp;
  } catch (error) {
    console.error('[Notifications] Failed to initialize Firebase Admin:', error);
    return null;
  }
}

const supabase = createClient(
  process.env.SUPABASE_URL,
  process.env.SUPABASE_ANON_KEY
);

async function fetchUserTokens(userId) {
  if (!notificationsEnabled) return [];

  const { data, error } = await supabase
    .from('user_fcm_tokens')
    .select('id, fcm_token, last_weather_alert_at')
    .eq('user_id', userId);

  if (error) {
    console.error('[Notifications] Failed to load tokens:', error);
    return [];
  }
  return data || [];
}

async function sendMessage(token, payload) {
  if (!notificationsEnabled) return false;
  const app = getFirebaseApp();
  if (!app) return false;

  try {
    await admin.messaging().send({
      token,
      ...payload
    });
    return true;
  } catch (error) {
    console.error('[Notifications] Failed to send message:', error);
    if (error.code === 'messaging/registration-token-not-registered') {
      await supabase.from('user_fcm_tokens').delete().eq('fcm_token', token);
    }
    return false;
  }
}

async function sendWeatherAlert(userId, trailName, weatherData) {
  const tokens = await fetchUserTokens(userId);
  const severeWeather = weatherData?.weather?.[0]?.main || 'Severe weather';
  const temperature = weatherData?.main?.temp;

  for (const tokenRecord of tokens) {
    const lastAlert = tokenRecord.last_weather_alert_at
      ? new Date(tokenRecord.last_weather_alert_at).getTime()
      : 0;
    const oneHourAgo = Date.now() - 60 * 60 * 1000;
    if (lastAlert > oneHourAgo) {
      continue;
    }

    const payload = {
      notification: {
        title: `⚠️ Weather Alert for ${trailName}`,
        body: `${severeWeather}${temperature ? ` • ${temperature}°C` : ''}`,
        icon: 'ic_notification'
      },
      data: {
        type: 'weather_alert',
        trail_name: trailName
      }
    };

    const sent = await sendMessage(tokenRecord.fcm_token, payload);
    if (sent) {
      await supabase
        .from('user_fcm_tokens')
        .update({ last_weather_alert_at: new Date().toISOString() })
        .eq('id', tokenRecord.id);
    }
  }
}

async function sendNewTrailNotification(userId, trailData) {
  const tokens = await fetchUserTokens(userId);
  for (const tokenRecord of tokens) {
    const payload = {
      notification: {
        title: 'New trail nearby!',
        body: `${trailData.name} just landed near you.`,
        icon: 'ic_notification'
      },
      data: {
        type: 'new_trail',
        trail_id: trailData.id,
        trail_name: trailData.name,
        city: trailData.city || ''
      }
    };
    await sendMessage(tokenRecord.fcm_token, payload);
  }
}

async function sendFriendActivity(userId, friendName, activity) {
  const tokens = await fetchUserTokens(userId);
  for (const tokenRecord of tokens) {
    const payload = {
      notification: {
        title: `${friendName} shared an update`,
        body: activity,
        icon: 'ic_notification'
      },
      data: {
        type: 'friend_review',
        friend_name: friendName,
        activity
      }
    };
    await sendMessage(tokenRecord.fcm_token, payload);
  }
}

module.exports = {
  sendWeatherAlert,
  sendNewTrailNotification,
  sendFriendActivity
};

