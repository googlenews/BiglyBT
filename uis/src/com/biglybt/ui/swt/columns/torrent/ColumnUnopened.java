/*
 * Created on Sep 19, 2008
 *
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA
 */

package com.biglybt.ui.swt.columns.torrent;


import org.eclipse.swt.graphics.Image;

import com.biglybt.core.download.DownloadManager;
import com.biglybt.ui.swt.pifimpl.UISWTGraphicImpl;
import com.biglybt.ui.swt.views.table.CoreTableColumnSWT;

import com.biglybt.core.torrent.PlatformTorrentUtils;
import com.biglybt.ui.common.table.TableRowCore;
import com.biglybt.ui.swt.imageloader.ImageLoader;

import com.biglybt.pif.download.Download;
import com.biglybt.pif.ui.menus.MenuItem;
import com.biglybt.pif.ui.menus.MenuItemListener;
import com.biglybt.pif.ui.tables.*;

/**
 * @author TuxPaper
 * @created Sep 19, 2008
 *
 */
public class ColumnUnopened
	extends CoreTableColumnSWT
	implements TableCellAddedListener, TableCellRefreshListener,
	TableCellMouseListener
{
	public static final Class<?> DATASOURCE_TYPE = Download.class;

	public static final String COLUMN_ID = "unopened";

	@Override
	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] { CAT_CONTENT, CAT_ESSENTIAL });
		info.setProficiency(TableColumnInfo.PROFICIENCY_BEGINNER);
	}

	private static UISWTGraphicImpl graphicCheck;
	private static UISWTGraphicImpl graphicUnCheck;
	private static UISWTGraphicImpl[] graphicsProgress;

	private static final int WIDTH = 38; // enough to fit title

	public ColumnUnopened(String tableID) {
		super(COLUMN_ID, tableID);

		synchronized( ColumnUnopened.class ){

			if (graphicCheck == null) {
				Image img = ImageLoader.getInstance().getImage("image.unopened");
				graphicCheck = new UISWTGraphicImpl(img);
			}
			if (graphicUnCheck == null) {
				Image img = ImageLoader.getInstance().getImage("image.opened");
				graphicUnCheck = new UISWTGraphicImpl(img);
			}

			if (graphicsProgress == null) {

				Image[] imgs = ImageLoader.getInstance().getImages("image.sidebar.vitality.dl");
				graphicsProgress = new UISWTGraphicImpl[imgs.length];
				for(int i = 0 ; i < imgs.length ; i++) {
					graphicsProgress[i] = new UISWTGraphicImpl(imgs[i]);
				}

			}
		}

		TableContextMenuItem menuItem = addContextMenuItem("label.toggle.new.marker");

		menuItem.addMultiListener(new MenuItemListener() {
			@Override
			public void selected(MenuItem menu, Object target) {
				Object[] dataSources = (Object[])target;

				for ( Object _ds: dataSources ){

					if (_ds instanceof TableRowCore) {
						TableRowCore row = (TableRowCore) _ds;
						_ds = row.getDataSource(true);
					}

					if ( _ds instanceof DownloadManager ){

						DownloadManager dm = (DownloadManager)_ds;

						boolean x = PlatformTorrentUtils.getHasBeenOpened( dm );

						PlatformTorrentUtils.setHasBeenOpened(dm, !x );
					}
				}
			}
		});

		initializeAsGraphic(WIDTH);
	}

	// @see com.biglybt.pif.ui.tables.TableCellAddedListener#cellAdded(com.biglybt.pif.ui.tables.TableCell)
	@Override
	public void cellAdded(TableCell cell) {
		cell.setMarginWidth(0);
		cell.setMarginHeight(0);
	}

	// @see com.biglybt.pif.ui.tables.TableCellRefreshListener#refresh(com.biglybt.pif.ui.tables.TableCell)
	@Override
	public void refresh(TableCell cell) {
		DownloadManager dm = (DownloadManager) cell.getDataSource();
		if (dm == null) {
			return;
		}
		int sortVal;
		boolean complete = dm.getAssumedComplete();
		boolean hasBeenOpened = false;
		if (complete) {
			hasBeenOpened = PlatformTorrentUtils.getHasBeenOpened(dm);
			sortVal = hasBeenOpened ? 1 : 0;
		} else {
			sortVal = isSortAscending()?2:-1;
		}

		if (!cell.setSortValue(sortVal) && cell.isValid()) {
			if(complete) {
				return;
			}
		}
		if (!cell.isShown()) {
			return;
		}

		if (complete) {
			cell.setGraphic(hasBeenOpened ? graphicUnCheck : graphicCheck);
		} else {
			if(dm.getState() == DownloadManager.STATE_DOWNLOADING) {
				int i = TableCellRefresher.getRefreshIndex(1, graphicsProgress.length);
				cell.setGraphic(graphicsProgress[i]);
				TableCellRefresher.addCell(this, cell);
			} else {
				cell.setGraphic(null);
			}

		}
	}

	// @see com.biglybt.pif.ui.tables.TableCellMouseListener#cellMouseTrigger(com.biglybt.pif.ui.tables.TableCellMouseEvent)
	@Override
	public void cellMouseTrigger(TableCellMouseEvent event) {
		if (event.eventType == TableRowMouseEvent.EVENT_MOUSEUP && event.button == 1) {
			DownloadManager dm = (DownloadManager) event.cell.getDataSource();
			boolean complete = dm.getAssumedComplete();
			if(!complete) return;
			boolean hasBeenOpened = !PlatformTorrentUtils.getHasBeenOpened(dm);
			PlatformTorrentUtils.setHasBeenOpened(dm, hasBeenOpened);
			event.cell.setGraphic(hasBeenOpened ? graphicUnCheck : graphicCheck);
			event.cell.invalidate();
		}
	}
}
