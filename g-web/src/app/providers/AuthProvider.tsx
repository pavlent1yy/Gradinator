'use client';

import React, {
    createContext,
    useCallback,
    useContext,
    useEffect,
    useState
} from 'react';

import * as api from '../../lib/api';
import type { Me } from '../../lib/api';

type AuthContextValue = {
    user: Me | null;
    initializing: boolean;
    authLoading: boolean;
    authError: string | null;
    login: (
        email: string,
        password: string
    ) => Promise<boolean>;
    logout: () => Promise<void>;
    refreshUser: () => Promise<void>;
    clearAuthError: () => void;
};

const AuthContext =
    createContext<AuthContextValue | null>(null);

export function AuthProvider({
    children
}: {
    children: React.ReactNode;
}) {

    const [user, setUser] =
        useState<Me | null>(null);

    const [initializing, setInitializing] =
        useState(true);

    const [authLoading, setAuthLoading] =
        useState(false);

    const [authError, setAuthError] =
        useState<string | null>(null);

    const restoreSession = useCallback(async () => {

        try {
            const me = await api.fetchMe();

            setUser(me);

            return;

        } catch {
            // access token отсутствует
            // или истёк
        }

        try {

            await api.refreshTokens();

            const me = await api.fetchMe();

            setUser(me);

        } catch {

            setUser(null);

        }

    }, []);

    useEffect(() => {

        (async () => {

            await restoreSession();

            setInitializing(false);

        })();

    }, [restoreSession]);

    const login = useCallback(async (
        email: string,
        password: string
    ) => {

        setAuthLoading(true);
        setAuthError(null);

        try {

            await api.login({
                email,
                password
            });

            const me = await api.fetchMe();

            setUser(me);

            return true;

        } catch (e: any) {

            setAuthError(
                e.message ?? 'Не удалось войти'
            );

            return false;

        } finally {

            setAuthLoading(false);

        }

    }, []);

    const logout = useCallback(async () => {

        await api.logout();

        setUser(null);

    }, []);

    const refreshUser = useCallback(async () => {
        setUser(await api.fetchMe());
    }, []);

    const clearAuthError = useCallback(
        () => setAuthError(null),
        []
    );

    const value = React.useMemo(
        () => ({
            user,
            initializing,
            authLoading,
            authError,
            login,
            logout,
            refreshUser,
            clearAuthError
        }),
        [
            user,
            initializing,
            authLoading,
            authError,
            login,
            logout,
            refreshUser,
            clearAuthError
        ]
    );

    return (
        <AuthContext.Provider value={value}>
            {children}
        </AuthContext.Provider>
    );
}

export function useAuthContext() {

    const ctx = useContext(AuthContext);

    if (!ctx) {
        throw new Error(
            'useAuthContext must be used within AuthProvider'
        );
    }

    return ctx;
}