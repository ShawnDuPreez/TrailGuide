import * as AuthSession from 'expo-auth-session';
import { supabase } from './supabase';

const SCHEME = 'trailguide-expo53';
const REDIRECT_PATH = 'auth/callback';

export function getRedirectUri() {
  return AuthSession.makeRedirectUri({
  scheme: 'trailguide-expo53',
  path: 'auth/callback',
  preferLocalhost: false,   // avoid localhost variants
});
}

export async function signInWithGoogle() {
  const redirectTo = getRedirectUri();
  const { data, error } = await supabase.auth.signInWithOAuth({
    provider: 'google',
    options: {
      redirectTo,
      scopes: 'openid profile email',
      queryParams: { access_type: 'offline', prompt: 'consent' },
    },
  });
  if (error) throw error;
  return { authUrl: data?.url ?? null, redirectTo };
}

export async function exchangeCodeForSession(callbackUrl) {
  try {
    if (!callbackUrl) return null;
    const u = new URL(callbackUrl);
    const err = u.searchParams.get('error') || u.searchParams.get('error_description');
    if (err) throw new Error(err);

    const code = u.searchParams.get('code');
    if (!code) return null;

    const { data, error } = await supabase.auth.exchangeCodeForSession({
      authCode: code,
      redirectTo: getRedirectUri(),
    });
    if (error) throw error;
    return data?.session ?? null;
  } catch {
    return null;
  }
}

export async function getCurrentUser() {
  const { data } = await supabase.auth.getUser();
  return data?.user ?? null;
}

export async function signOut() {
  const { error } = await supabase.auth.signOut();
  if (error) throw error;
}
