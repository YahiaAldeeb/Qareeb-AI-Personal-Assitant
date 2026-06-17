"""
Portal Bearer token support so /state_full and /keyboard/input return 200 instead of 401.
Token is read from PORTAL_AUTH_TOKEN in .env, or from device via content query.
"""
import base64
import json
import logging
import os
import re

import httpx
from dotenv import load_dotenv

load_dotenv()

logger = logging.getLogger(__name__)

# Lazy import to avoid circular deps and patch only when needed
_PortalClient = None
_original_get_state_tcp = None
_original_input_text_tcp = None
_original_take_screenshot_tcp = None
_patched = False


def _get_portal_client():
    global _PortalClient
    if _PortalClient is None:
        from droidrun.tools.android.portal_client import PortalClient
        _PortalClient = PortalClient
    return _PortalClient


def _auth_headers(portal_instance) -> dict:
    token = getattr(portal_instance, "_auth_token", None)
    if not token:
        return {}
    return {"Authorization": f"Bearer {token}"}


async def _patched_get_state_tcp(self):
    await self._ensure_connected()
    headers = _auth_headers(self)
    try:
        async with httpx.AsyncClient() as client:
            response = await client.get(
                f"{self.tcp_base_url}/state_full",
                timeout=10,
                headers=headers or None,
            )
            if response.status_code == 200:
                data = response.json()
                if isinstance(data, dict):
                    inner_key = (
                        "result"
                        if "result" in data
                        else "data" if "data" in data else None
                    )
                    if inner_key:
                        inner_value = data[inner_key]
                        if isinstance(inner_value, str):
                            try:
                                return json.loads(inner_value)
                            except json.JSONDecodeError:
                                pass
                        elif isinstance(inner_value, dict):
                            return inner_value
                return data
            if response.status_code == 401:
                logger.error("Portal state_full returned 401 - auth token missing or invalid")
            return await self._get_state_content_provider()
    except Exception as e:
        logger.debug(f"TCP get_state error: {e}, using fallback")
        return await self._get_state_content_provider()


async def _patched_input_text_tcp(self, text: str, clear: bool) -> bool:
    await self._ensure_connected()
    headers = _auth_headers(self)
    try:
        encoded = base64.b64encode(text.encode()).decode()
        payload = {"base64_text": encoded, "clear": clear}
        req_headers = {"Content-Type": "application/json"}
        if headers:
            req_headers.update(headers)
        async with httpx.AsyncClient() as client:
            response = await client.post(
                f"{self.tcp_base_url}/keyboard/input",
                json=payload,
                headers=req_headers,
                timeout=10,
            )
            if response.status_code == 200:
                logger.debug("TCP input_text successful")
                return True
            if response.status_code == 401:
                logger.error("Portal keyboard/input returned 401 - auth token missing or invalid")
            return await self._input_text_content_provider(text, clear)
    except Exception as e:
        logger.debug(f"TCP input_text error: {e}, using fallback")
        return await self._input_text_content_provider(text, clear)


async def _patched_take_screenshot_tcp(self, hide_overlay: bool) -> bytes:
    await self._ensure_connected()
    headers = _auth_headers(self)
    try:
        url = f"{self.tcp_base_url}/screenshot"
        if not hide_overlay:
            url += "?hideOverlay=false"
        async with httpx.AsyncClient() as client:
            response = await client.get(
                url,
                timeout=10.0,
                headers=headers or None,
            )
            if response.status_code == 200:
                data = response.json()
                if data.get("status") == "success":
                    inner_key = (
                        "result"
                        if "result" in data
                        else "data" if "data" in data else None
                    )
                    if inner_key:
                        logger.debug("Screenshot taken via TCP")
                        return base64.b64decode(data[inner_key])
            if response.status_code == 401:
                logger.error("Portal screenshot returned 401 - auth token missing or invalid")
            return await self._take_screenshot_adb()
    except Exception as e:
        logger.debug(f"TCP screenshot error: {e}, using fallback")
        return await self._take_screenshot_adb()


def _apply_portal_auth_patch():
    """Patch PortalClient so TCP requests include Authorization: Bearer <token> when _auth_token is set."""
    global _patched
    if _patched:
        return
    PortalClient = _get_portal_client()
    PortalClient._get_state_tcp = _patched_get_state_tcp
    PortalClient._input_text_tcp = _patched_input_text_tcp
    PortalClient._take_screenshot_tcp = _patched_take_screenshot_tcp
    _patched = True
    logger.debug("PortalClient patched for Bearer token auth")


async def fetch_portal_token(device) -> str | None:
    """
    Read Portal auth token from the device.
    Run: content query --uri content://com.droidrun.portal/auth_token
    """
    try:
        out = await device.shell("content query --uri content://com.droidrun.portal/auth_token")
        if not out or not out.strip():
            logger.warning("Portal auth_token content query returned empty")
            return None
        # Parse "Row: 0 token=xxxx" or similar
        for line in out.strip().splitlines():
            line = line.strip()
            if "token=" in line:
                m = re.search(r"token=([^\s,]+)", line)
                if m:
                    token = m.group(1).strip()
                    if token:
                        logger.info("Portal auth token fetched successfully")
                        return token
        logger.warning("Could not parse token from auth_token query: %s", out[:200])
        return None
    except Exception as e:
        logger.exception("Failed to fetch Portal auth token: %s", e)
        return None


async def set_portal_auth(tools) -> bool:
    """
    Set Portal Bearer token on tools.portal.
    Uses PORTAL_AUTH_TOKEN from .env if set; otherwise fetches from device.
    Call this after tools.connect(). Applies the Bearer-token patch if not already applied.
    """
    _apply_portal_auth_patch()
    if not getattr(tools, "device", None) or not getattr(tools, "portal", None):
        logger.warning("set_portal_auth: tools has no device or portal")
        return False
    token = (os.environ.get("PORTAL_AUTH_TOKEN") or "").strip()
    if not token:
        token = await fetch_portal_token(tools.device)
    if not token:
        logger.warning("Portal auth token not available - state_full and keyboard/input may return 401")
        return False
    tools.portal._auth_token = token
    logger.info("Portal Bearer token set; state_full and keyboard/input should succeed")
    return True
