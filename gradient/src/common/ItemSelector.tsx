import { useEffect, useMemo, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { useQuery } from 'react-query';
import { CountSelectorModal, useMcData } from '.';
import { getInventoryListing } from '../api/automation';
import styled, { css } from 'styled-components';
import { ExtendedItem, itemListFromInventories } from '../helpers';
import { Fzf } from 'fzf';
import { Item } from '../api/types';

export type SelectedItemsFinal = {
  item: ExtendedItem;
  shulkerCount: number;
  itemCount: number;
}[];
export type SelectedItems = {
  [hashCode: string]: { shulkerCount: number; itemCount: number } | undefined;
};
type Props = {
  submit: (final: SelectedItemsFinal) => void;
  selectedItems: SelectedItems;
  setSelectedItems: (upd_func: (items: SelectedItems) => SelectedItems) => void;
};

const itemCompareTieBreaker = (a: Item, b: Item) => {
  if (b.count > a.count) {
    return 1;
  } else if (b.count < a.count) {
    return -1;
  }

  return b.stackable_hash.localeCompare(a.stackable_hash);
};

export const ItemSelector = ({
  selectedItems,
  setSelectedItems,
  submit,
}: Props) => {
  const mcData = useMcData();
  const [hoverIdx, setHoverIdx] = useState(0);
  const [search, setSearch] = useState('');
  const [modalItem, setModalItem] = useState<ExtendedItem | null>();
  const mainInputRef = useRef<HTMLInputElement>(null);

  const setSelectedForItem = (
    item: Item,
    request: { shulkerCount: number; itemCount: number },
  ) => {
    setSelectedItems((currSelected) => ({
      ...currSelected,
      [item.stackable_hash]: request,
    }));
  };

  const { isLoading, isError, data } = useQuery(
    'inventory_listing',
    getInventoryListing,
    { refetchInterval: 1000 * 5 },
  );

  const itemList = itemListFromInventories(mcData, data?.data ?? []);
  const fzf = useMemo(
    () =>
      new Fzf(itemList, {
        selector: (item) => item.prettyPrinted,
        tiebreakers: [(a, b) => itemCompareTieBreaker(a.item, b.item)],
      }),
    [itemList],
  );

  const items =
    search.length > 0
      ? fzf.find(search).map((res) => res.item)
      : itemList.sort((a, b) => {
          const aRequest = selectedItems[a.stackable_hash];
          const bRequest = selectedItems[b.stackable_hash];
          const aSelected = Boolean(
            aRequest && (aRequest.shulkerCount > 0 || aRequest.itemCount > 0),
          );
          const bSelected = Boolean(
            bRequest && (bRequest.shulkerCount > 0 || bRequest.itemCount > 0),
          );

          if (aSelected && !bSelected) {
            return -1;
          } else if (bSelected && !aSelected) {
            return 1;
          }

          return itemCompareTieBreaker(a, b);
        });

  const submitSelected = () => {
    const selected = [];

    for (const [hashCode, request] of Object.entries(selectedItems)) {
      if (!request || (request.shulkerCount === 0 && request.itemCount === 0))
        continue;
      const matchedItem = itemList.find(
        (item) => item.stackable_hash === hashCode,
      );

      if (!matchedItem) continue;

      selected.push({
        item: matchedItem,
        shulkerCount: request.shulkerCount,
        itemCount: request.itemCount,
      });
    }

    submit(selected);
  };

  const clampHoverIdx = (idx: number) =>
    Math.max(0, Math.min(idx, items.length - 1));

  useEffect(() => {
    setHoverIdx(clampHoverIdx(hoverIdx));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [items.length]);

  useEffect(() => {
    if (Boolean(modalItem)) return;

    mainInputRef.current?.focus();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [Boolean(modalItem)]);

  if (isLoading) return <p>Loading inventory contents</p>;
  if (isError) return <p>Failed to load inventory contents</p>;

  const onKeyDown = (ev: React.KeyboardEvent<HTMLInputElement>) => {
    if (ev.key === 'ArrowUp' || ev.key === 'ArrowDown') {
      const up = ev.key === 'ArrowUp';

      setHoverIdx(clampHoverIdx(hoverIdx + (up ? -1 : 1)));

      ev.preventDefault();
    }

    if (ev.key === 'Enter') {
      if (ev.shiftKey) {
        submitSelected();
        return;
      }

      const currentItem = items[hoverIdx];

      setModalItem(currentItem);
      console.log(currentItem);
    }
  };

  return (
    <Container>
      <UpperContainer>
        <ItemInput
          autoFocus={!Boolean(modalItem)}
          ref={mainInputRef}
          type="text"
          disabled={Boolean(modalItem)}
          onKeyDown={onKeyDown}
          value={search}
          onChange={({ target: { value } }) => setSearch(value || '')}
        />

        <button onClick={submitSelected}>Submit</button>
      </UpperContainer>
      <InnerContainer>
        <ItemList>
          {items.map((item, idx) => {
            const selectedRequest = selectedItems[item.stackable_hash];
            const hasSelection =
              selectedRequest &&
              (selectedRequest.shulkerCount > 0 ||
                selectedRequest.itemCount > 0);

            return (
              <ItemOption
                key={idx}
                hovered={hoverIdx === idx}
                ref={
                  hoverIdx === idx
                    ? (elem) => elem?.scrollIntoView({ block: 'nearest' })
                    : undefined
                }
                onClick={() => setModalItem(item)}
              >
                {item.prettyPrinted} x{item.count.toLocaleString()}
                {hasSelection && (
                  <SelectedText hovered={hoverIdx === idx}>
                    (
                    {selectedRequest?.shulkerCount > 0
                      ? `${selectedRequest.shulkerCount} shulkers`
                      : `${selectedRequest?.itemCount} items`}{' '}
                    selected)
                  </SelectedText>
                )}
              </ItemOption>
            );
          })}
        </ItemList>
      </InnerContainer>
      {modalItem &&
        createPortal(
          <ModalContainer>
            <CountSelectorModal
              item={modalItem}
              currentRequest={selectedItems[modalItem.stackable_hash]}
              close={(request) => {
                if (request !== null) {
                  setSelectedForItem(modalItem, request);
                  setSearch('');
                }

                setModalItem(null);
              }}
            />
          </ModalContainer>,
          document.body,
        )}
    </Container>
  );
};

const Container = styled.div`
  display: grid;
  min-height: 0;
  grid-template-rows: 25px 1fr;
  grid-template-columns: 1fr;
`;

const InnerContainer = styled.div`
  overflow-y: auto;
`;

const ItemInput = styled.input`
  flex-grow: 1;
  outline: none;
`;

const UpperContainer = styled.div`
  display: flex;
`;

const ItemList = styled.ul`
  margin: 0;
  padding: 0;
  list-style: none;
`;

const ItemOption = styled.li<{ hovered: boolean }>`
  padding: 4px;

  ${({ hovered }) =>
    hovered
      ? css`
          background-color: ${({ theme }) => theme.fg0};
          color: ${({ theme }) => theme.bg0};
        `
      : ''};
`;

const SelectedText = styled.span<{ hovered: boolean }>`
  margin-left: 2em;
  color: ${({ theme, hovered }) => (hovered ? theme.red : theme.purple)};
`;

const ModalContainer = styled.div`
  position: fixed;
  width: 100vw;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(0, 0, 0, 0.5);
`;
