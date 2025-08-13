import * as AuthSession from 'expo-auth-session';
import { supabase } from './supabase';

export function getRedirectUri() {
  return AuthSession.makeRedirectUri({ scheme: 'trailguide-expo53', path: 'auth/callback' });
}

export async function signInWithGoogle() {
  const redirectTo = getRedirectUri();
  const { data, error } = await supabase.auth.signInWithOAuth({
    provider: 'google',
    options: { redirectTo, queryParams: { access_type: 'offline', prompt: 'consent' } },
  });
  if (error) throw error;
  return { authUrl: data?.url, redirectTo };
}

export async function exchangeCodeForSession(url) {
  // Extract ?code= from the redirect URL and exchange for a session
  try {
    const code = new URL(url).searchParams.get('code');
    if (!code) return null;
    const { data, error } = await supabase.auth.exchangeCodeForSession({ code });
    if (error) throw error;
    return data?.session ?? null;
  } catch (e) {
    return null;
  }
}

export async function signOut() {
  const { error } = await supabase.auth.signOut();
  if (error) throw error;
}

export async function getCurrentUser() {
  const { data } = await supabase.auth.getUser();
  return data?.user ?? null;
}

