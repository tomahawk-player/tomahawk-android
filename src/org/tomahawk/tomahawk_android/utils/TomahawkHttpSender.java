/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2015, Enno Gottschalk <mrmaffen@googlemail.com>
 *
 *   Tomahawk is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Tomahawk is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Tomahawk. If not, see <http://www.gnu.org/licenses/>.
 */
package org.tomahawk.tomahawk_android.utils;

import org.acra.ReportField;
import org.acra.collector.CrashReportData;
import org.acra.sender.HttpSender;
import org.acra.sender.ReportSenderException;
import org.tomahawk.tomahawk_android.dialogs.SendLogConfigDialog;

import java.lang.reflect.Method;
import java.util.Map;

public class TomahawkHttpSender extends HttpSender {

    public TomahawkHttpSender(Method method, Type type,
            Map<ReportField, String> mapping) {
        super(method, type, mapping);
    }

    @Override
    public void send(CrashReportData data) throws ReportSenderException {
        if (!"org.tomahawk.tomahawk_android".equals(data.getProperty(ReportField.PACKAGE_NAME))) {
            return;
        }

        if (data.getProperty(ReportField.STACK_TRACE)
                .startsWith(SendLogConfigDialog.SendLogException.getDefaultString())) {
            // this means that it's a manually send crash report through the "send log"-feature
            data.put(ReportField.USER_EMAIL, SendLogConfigDialog.mLastEmail);
            data.put(ReportField.USER_COMMENT, SendLogConfigDialog.mLastUsermessage);
        }
        super.send(data);
    }
}
