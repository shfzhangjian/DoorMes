/** Build-time configuration exposed to the DoorMes browser composition root. */
interface ImportMetaEnv {
  /**
   * Authenticated backend base URL for managed visual assets.
   *
   * Development defaults to `/api`, whose Vite adapter writes into the
   * configured local directory. Production should set an authenticated URL;
   * an empty production value retains the IndexedDB offline fallback.
   * @since 0.10.14
   */
  readonly VITE_VISUAL_ASSET_API_BASE_URL?: string;
  /** True only while Vite serves the local development application. */
  readonly DEV: boolean;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

/** Allows the Vite entry to import its side-effect stylesheet under strict TS. */
declare module "*.css";
